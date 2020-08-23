package com.uploader.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.zeroturnaround.zip.ZipUtil;

import com.uploader.dto.ResourceDTO;
import com.uploader.entity.Document;
import com.uploader.entity.Resource;
import com.uploader.entity.Task;
import com.uploader.repository.DocumentRepository;
import com.uploader.repository.ResourceRepository;
import com.uploader.repository.TaskRepository;
import com.uploader.utils.CommonUtils;
import com.uploader.utils.ProjectEnums;
import com.uploader.utils.ProjectEnums.ResourceType;
import com.uploader.utils.TimeUtils;

@Service
public class ResourceService {
	
	@Value(value = "${resources_base_location}")
	private String resourcesBaseLocation;
	
	@Value(value = "${temp_location}")
	private String tempLocation;
	
	@Value(value = "${html_location}")
	private String htmlLocation;
	
	@Autowired
	private ResourceRepository resourceRepository;
	
	@Autowired
	private TaskRepository taskRepository;
	
	@Autowired
	private DocumentRepository documentRepository;
	
	@Autowired
	private AsyncService asyncService; 
	
	
	public List<ResourceDTO> getBreadcrumb(Long folderId) {
		
		List<ResourceDTO> response = new ArrayList<ResourceDTO>();
		this.getChainedResources(folderId, response);
		
		return response;
	}
	
	public List<ResourceDTO> getAllResources(Long parentResourceId, HttpServletRequest request) {
		
		List<ResourceDTO> response = new ArrayList<ResourceDTO>();
		
		List<Resource> resources = new ArrayList<Resource>();
		
		if(Objects.isNull(parentResourceId)) {
			resources = this.resourceRepository.findAllByParentResourceIsNull();
		}else {
			Resource parentResource = this.resourceRepository.findById(parentResourceId).get();
			resources = this.resourceRepository.findAllByParentResource(parentResource);
		}
		
		if(Objects.nonNull(resources) && !resources.isEmpty()) {
			for(Resource resource : resources) {
				ResourceDTO resourceDTO = new ResourceDTO();
				resourceDTO.setResourceId(resource.getResourceId());
				resourceDTO.setResourceType(resource.getType().name());
				resourceDTO.setResourceOriginalName(resource.getOriginalName());
				resourceDTO.setCreateDateTime(resource.getCreateDateTime());
				resourceDTO.setLastAccessedDateTime(resource.getLastModifiedDateTime());
				resourceDTO.setSizeInBytes(resource.getSizeInBytes());
				resourceDTO.setParentResourceId(Objects.nonNull(resource.getParentResource()) ? resource.getParentResource().getResourceId() : null);
				response.add(resourceDTO);
			}
		}
		
		this.sortResources(response);
		return response;
	}
	
	@Transactional
	public ResourceDTO addResource(ResourceDTO resourceFromRequest, HttpServletRequest request) throws IOException {
		
		ResourceDTO response = new ResourceDTO();
		if(resourceFromRequest.getResourceType().equalsIgnoreCase(ProjectEnums.ResourceType.FILE.name())) {
			response = this.uploadFileResource(resourceFromRequest);
			
			if(Objects.nonNull(resourceFromRequest.getParentResourceId())) {
				this.asyncService.updateFolderSizeRecursively(resourceFromRequest.getParentResourceId());
			}
		
		}else if(resourceFromRequest.getResourceType().equalsIgnoreCase(ProjectEnums.ResourceType.FOLDER.name())) {
			response = this.createFolderResource(resourceFromRequest);
			if(Objects.nonNull(resourceFromRequest.getParentResourceId())) {
				this.asyncService.updateFolderSizeRecursively(resourceFromRequest.getParentResourceId());
			}
		}
		
		return response;
	}
	
	public String viewResourceHTML(Long resourceId) {
		Document doc = this.documentRepository.findByResourceId(resourceId);
		
		if(Objects.isNull(doc)) {
			return "No Preview Available";
		}
    	StringBuilder contentBuilder = new StringBuilder();
    	try {
    		
    	    BufferedReader in = new BufferedReader(new FileReader(this.htmlLocation + doc.getHtmlDocPath()));
    	    String str;
    	    while ((str = in.readLine()) != null) {
    	        contentBuilder.append(str);
    	    }
    	    in.close();
    	} catch (IOException e) {
    	}
    	String content = contentBuilder.toString();
    	return content;
	}
	
	@Transactional
	public ResourceDTO importZipFile(ResourceDTO resourceFromRequest, HttpServletRequest request) throws IOException {
		
		String fileOriginalName = StringUtils.cleanPath(resourceFromRequest.getFile().getOriginalFilename());
		String temporarilySystemGeneratedFileName = TimeUtils.getCurrentUTCTimeInMilliseconds()+CommonUtils.RESOURCE_NAME_APPENDER+fileOriginalName;
		String temporaryFilePathAbsolute = this.storeZipFileOnTempLocation(resourceFromRequest.getFile(), temporarilySystemGeneratedFileName);
		
		String relativeImportFileLocation = this.createParentFolderToExtractZipInside(resourceFromRequest);
		
		Resource parentFolderToSave = new Resource();
		parentFolderToSave.setType(ResourceType.FOLDER);
		parentFolderToSave.setCreateDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
		parentFolderToSave.setLastModifiedDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
		parentFolderToSave.setFilesystemPath(relativeImportFileLocation);
		
		if(Objects.nonNull(resourceFromRequest.getParentResourceId())) {
			Resource parentResource = this.resourceRepository.findById(resourceFromRequest.getParentResourceId()).get();
			parentFolderToSave.setParentResource(parentResource);
		}
		
		int extensionStartIndex = FilenameUtils.indexOfExtension(fileOriginalName);
		String fileNameWithoutExtension = fileOriginalName.substring(0, extensionStartIndex);
		parentFolderToSave.setOriginalName(fileNameWithoutExtension);
		this.resourceRepository.save(parentFolderToSave);
		
		ZipUtil.unpack(new File(temporaryFilePathAbsolute), new File(this.resourcesBaseLocation+relativeImportFileLocation));
		
		this.createDBEntryForExtractedResources(relativeImportFileLocation);
		
		parentFolderToSave.setSizeInBytes(FileUtils.sizeOfDirectory(new File(this.resourcesBaseLocation+relativeImportFileLocation)));
		this.resourceRepository.save(parentFolderToSave);
		
		if(Objects.nonNull(resourceFromRequest.getParentResourceId())) {
			this.asyncService.updateFolderSizeRecursively(resourceFromRequest.getParentResourceId());
		}
		return null;
	}
	
	public void deleteResource(List<Long> ids) {
		
		if(Objects.nonNull(ids) && !ids.isEmpty()) {
			for(Long id : ids) {
				Resource resource = this.resourceRepository.findById(id).get();
				
				String resourcePath = this.resourcesBaseLocation + resource.getFilesystemPath();
				File fileToDelete = new File(resourcePath);
				FileSystemUtils.deleteRecursively(fileToDelete);
				
				List<Resource> entities = new ArrayList<Resource>();
				this.createStackAndRemoveAllEntries(resource, entities);
				
				Collections.reverse(entities);
				for(Resource entry : entities) {
					Document doc = this.documentRepository.findByResourceId(entry.getResourceId());
					if(Objects.nonNull(doc)) {
						this.documentRepository.delete(doc);
					}
					
					Task task = this.taskRepository.findByResourceId(entry.getResourceId());
					if(Objects.nonNull(task)) {
						this.taskRepository.delete(task);
					}
					
					this.resourceRepository.delete(entry);
				}
				
				if(Objects.nonNull(resource.getParentResource())) {
					this.asyncService.updateFolderSizeRecursively(resource.getParentResource().getResourceId());
				}
			}
		}
	}
	
	public ResponseEntity<org.springframework.core.io.Resource> download(ResourceDTO resourceDTO, HttpServletResponse response, HttpServletRequest request) {
		
		org.springframework.core.io.Resource resource = null;
		String fileName = null;
		
		if(resourceDTO.getResourceType().equals(ResourceType.FOLDER.name())) {
			String zipFileName = "download.zip";
			if(Objects.nonNull(resourceDTO.getResourceId())) {
				Resource parentResource = this.resourceRepository.findById(resourceDTO.getResourceId()).get();
				zipFileName = parentResource.getOriginalName() + ".zip";
			}
			String absolutePathToCompress = this.getAbsolutePathToCompress(resourceDTO);
			String absolutePathToTemporaryZipFile = this.createTemporaryZipFilePath(zipFileName);
			
			ZipUtil.pack(new File(absolutePathToCompress), new File(absolutePathToTemporaryZipFile));
			
			String attachedFileName = Paths.get(absolutePathToTemporaryZipFile).getFileName().toString();
			int separatorIndex = attachedFileName.indexOf(CommonUtils.RESOURCE_NAME_APPENDER);
			attachedFileName = attachedFileName.substring(separatorIndex+1);
			separatorIndex = attachedFileName.indexOf(CommonUtils.RESOURCE_NAME_APPENDER);
			attachedFileName = attachedFileName.substring(separatorIndex+1);
			
			fileName = attachedFileName;
			resource = new FileSystemResource(absolutePathToTemporaryZipFile);
		}else if(resourceDTO.getResourceType().equals(ResourceType.FILE.name())){
			String fileAbsolutePath = null;
			Resource fileFromDB = this.resourceRepository.findById(resourceDTO.getResourceId()).get();
			fileAbsolutePath = this.resourcesBaseLocation + fileFromDB.getFilesystemPath();
			
			fileName = fileFromDB.getOriginalName();
			resource = new FileSystemResource(fileAbsolutePath);
		}
	
		String contentType = null;
		try {
			contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			System.out.println("could not find content type");
		}

		if(contentType == null) {
			contentType = "application/octet-stream";
		}

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
				.body(resource);
	}

	private void getChainedResources(Long resourceId, List<ResourceDTO> resourceChain) {
		ResourceDTO chainedResource = new ResourceDTO();
		if(Objects.nonNull(resourceId)) {
			Resource resource = this.resourceRepository.findById(resourceId).get();
			chainedResource.setResourceId(resource.getResourceId());
			chainedResource.setResourceType(resource.getType().name());
			chainedResource.setResourceOriginalName(resource.getOriginalName());
			resourceChain.add(chainedResource);
			
			if(Objects.nonNull(resource.getParentResource())) {
				this.getChainedResources(resource.getParentResource().getResourceId(), resourceChain);
			}
		}
	}
	
	private void sortResources(List<ResourceDTO> resources) {
		List<String> definedOrder = Arrays.asList(ResourceType.FOLDER.name(), ResourceType.FILE.name());
		Comparator<ResourceDTO> resourceComparator = Comparator.comparing(c -> definedOrder.indexOf(((ResourceDTO)c).getResourceType()));
		Collections.sort(resources, resourceComparator);
	}

	/**
	 * Create a folder on file system, and persist database related
	 * @param folderResourceRequestDTO
	 * @throws IOException 
	 */
	private ResourceDTO createFolderResource(ResourceDTO folderResourceRequestDTO) throws IOException {
		ResourceDTO response = new ResourceDTO();
		
		Resource folderToSave = new Resource();
		folderToSave.setType(ResourceType.FOLDER);
		folderToSave.setCreateDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
		folderToSave.setLastModifiedDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
		folderToSave.setOriginalName(folderResourceRequestDTO.getResourceOriginalName());
		
		if(Objects.nonNull(folderResourceRequestDTO.getParentResourceId())) {
			Resource parentResource = this.resourceRepository.findById(folderResourceRequestDTO.getParentResourceId()).get();
			
			folderToSave.setFilesystemPath(parentResource.getFilesystemPath() + File.separator + folderResourceRequestDTO.getResourceOriginalName());
			folderToSave.setParentResource(parentResource);
		}else {
			folderToSave.setFilesystemPath(folderResourceRequestDTO.getResourceOriginalName());
		}
		
		String folderNameOnFS = this.getNonExistingIncrementedFolderName(folderToSave.getFilesystemPath());
		folderToSave.setFilesystemPath(folderNameOnFS);
		
		createFolderOnFileSystem(folderToSave.getFilesystemPath());
		
		folderToSave.setSizeInBytes(FileUtils.sizeOfDirectory(new File(this.resourcesBaseLocation + folderNameOnFS)));
		this.resourceRepository.save(folderToSave);
        
        response.setResourceId(folderToSave.getResourceId());
        response.setCreateDateTime(folderToSave.getCreateDateTime());
        response.setLastAccessedDateTime(folderToSave.getLastModifiedDateTime());
        response.setResourceOriginalName(folderToSave.getOriginalName());
		
		return response;
	}
	
	private String getNonExistingIncrementedFolderName(final String folderName) {
		
		String folderNameOnFS = folderName;
		
		Integer numberOfCopies = null;
		boolean run = true;
		while(run) {
			if(Objects.isNull(numberOfCopies)) {
				numberOfCopies = 1;
				run = this.ifFileExists(this.resourcesBaseLocation+folderNameOnFS);
			}else {
				folderNameOnFS = folderName+"("+numberOfCopies.toString()+")";
				
				run = this.ifFileExists(this.resourcesBaseLocation+folderNameOnFS);
				numberOfCopies++;
			}
		}
		
		return folderNameOnFS;
	}
	
	private void createFolderOnFileSystem(String relativeFolderPath) throws IOException {
		
		Path path = Paths.get(this.resourcesBaseLocation+relativeFolderPath);
		Files.createDirectory(path);
	}
	
	private ResourceDTO uploadFileResource(ResourceDTO fileResourceRequestDTO) throws IOException {
		
		ResourceDTO response = new ResourceDTO();
		
		Resource fileToSave = new Resource();
		fileToSave.setType(ResourceType.FILE);
		fileToSave.setCreateDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
		fileToSave.setLastModifiedDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
		
		if(Objects.nonNull(fileResourceRequestDTO.getFile())) {
			String fileOriginalName = StringUtils.cleanPath(fileResourceRequestDTO.getFile().getOriginalFilename());
			fileToSave.setSizeInBytes(fileResourceRequestDTO.getFile().getSize());
			fileToSave.setOriginalName(fileOriginalName);
			if(Objects.nonNull(fileResourceRequestDTO.getParentResourceId())) {
				Resource parentResource = this.resourceRepository.findById(fileResourceRequestDTO.getParentResourceId()).get();
				
				fileToSave.setFilesystemPath(parentResource.getFilesystemPath() + File.separator +  fileOriginalName);
				fileToSave.setParentResource(parentResource);
			}else {
				fileToSave.setFilesystemPath(fileOriginalName);
			}
		}
		
		String fileNameOnFS = this.getNonExistingIncrementedFileName(fileToSave.getFilesystemPath());
    	fileToSave.setFilesystemPath(fileNameOnFS);

    	this.saveFileOnFileSystem(fileResourceRequestDTO.getFile(), fileToSave.getFilesystemPath());
        this.resourceRepository.save(fileToSave);
        
        response.setResourceId(fileToSave.getResourceId());
        response.setCreateDateTime(fileToSave.getCreateDateTime());
        response.setLastAccessedDateTime(fileToSave.getLastModifiedDateTime());
        response.setResourceOriginalName(fileToSave.getOriginalName());
        response.setSizeInBytes(fileToSave.getSizeInBytes());
        this.createTaskForHTMLConversion(fileToSave);
        return response;
	}
	
	private void createTaskForHTMLConversion(Resource resource) {
		
		String fileName = resource.getOriginalName();
		String extension = FilenameUtils.getExtension(fileName);
		
		if(Objects.nonNull(extension) && (extension.equalsIgnoreCase("doc") || extension.equalsIgnoreCase("docx") || extension.equalsIgnoreCase("pdf")) ) {
			Task task = new Task();
	        task.setStatus("draft");
	        task.setTaskType("UPLOAD");
	        task.setCreationDate(new Date());
	        task.setResourceId(resource.getResourceId());

	        taskRepository.save(task);
		}
	}
	
	private String getNonExistingIncrementedFileName(final String fileName) {
		
		String fileNameOnFS = fileName;
		
		Integer numberOfCopies = null;
		boolean run = true;
		while(run) {
			if(Objects.isNull(numberOfCopies)) {
				numberOfCopies = 1;
				run = this.ifFileExists(this.resourcesBaseLocation+fileNameOnFS);
			}else {
				String fileExtension = FilenameUtils.getExtension(fileName);
				int extensionStartIndex = FilenameUtils.indexOfExtension(fileName);
				String tempFileNameWithoutExtension = fileName.substring(0, extensionStartIndex);
				fileNameOnFS = tempFileNameWithoutExtension+"("+numberOfCopies.toString()+")."+fileExtension;
				
				run = this.ifFileExists(this.resourcesBaseLocation+fileNameOnFS);
				numberOfCopies++;
			}
		}
		
		return fileNameOnFS;
	}
	
	private boolean ifFileExists(String absoluteFilePath) {
		File temp = new File(absoluteFilePath);
		return temp.exists();
	}
	
	private void saveFileOnFileSystem(MultipartFile file, String fileRelativePath) throws IOException {
		Path fileStorageLocationPath = Paths.get(this.resourcesBaseLocation+fileRelativePath);
		Files.copy(file.getInputStream(), fileStorageLocationPath, StandardCopyOption.REPLACE_EXISTING);
	}
	
	private void createDBEntryForExtractedResources(String relativeImportFileLocation) {
		try {
			Stream<Path> paths = Files.walk(Paths.get(this.resourcesBaseLocation+relativeImportFileLocation));
			
			List<Resource> extractedFiles = new ArrayList<Resource>();
			
			paths.forEach(filePath -> {
				Resource resourceToSave = new Resource();
				resourceToSave.setCreateDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
				resourceToSave.setLastModifiedDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
				resourceToSave.setOriginalName(filePath.getFileName().toString());
				
				String absolutePath = filePath.toFile().getPath();
				
				if(!absolutePath.equals(this.resourcesBaseLocation+relativeImportFileLocation)) {
					String relativePath = absolutePath.replaceAll(this.resourcesBaseLocation, "");
					resourceToSave.setFilesystemPath(relativePath);
					
					ResourceType resourceType = filePath.toFile().isDirectory() ? ResourceType.FOLDER : ResourceType.FILE;
					resourceToSave.setType(resourceType);
					
					if(resourceToSave.getType().equals(ResourceType.FILE)) {
						resourceToSave.setSizeInBytes(filePath.toFile().length());
						extractedFiles.add(resourceToSave);
					}else if(resourceToSave.getType().equals(ResourceType.FOLDER)) {
						resourceToSave.setSizeInBytes(FileUtils.sizeOfDirectory(filePath.toFile()));
					}
					
					String parentDirectoryRelativePath = filePath.toFile().getParent();
					parentDirectoryRelativePath = parentDirectoryRelativePath.replaceAll(this.resourcesBaseLocation, "");
					if(!parentDirectoryRelativePath.equals(this.resourcesBaseLocation)) {
						Resource parentResource = this.resourceRepository.findByFilesystemPath(parentDirectoryRelativePath);
						resourceToSave.setParentResource(parentResource);
					}
					
					this.resourceRepository.save(resourceToSave);
				}
				
			});
			
			for(Resource file : extractedFiles) {
				this.createTaskForHTMLConversion(file);
			}
			
			paths.close();
		}catch(Exception e) {
			
		}
	}
	
	private String createParentFolderToExtractZipInside(ResourceDTO resourceFromRequest) throws IOException {
		
		String fileOriginalName = StringUtils.cleanPath(resourceFromRequest.getFile().getOriginalFilename());
		
		int extensionStartIndex = FilenameUtils.indexOfExtension(fileOriginalName);
		String fileNameWithoutExtension = fileOriginalName.substring(0, extensionStartIndex);
		
		Long parentResourceId = resourceFromRequest.getParentResourceId();
		String relativeImportFileLocation = fileNameWithoutExtension;
		if(Objects.nonNull(parentResourceId)) {
			Resource parentResource = this.resourceRepository.findById(parentResourceId).get();
			relativeImportFileLocation = parentResource.getFilesystemPath() + File.separator + fileNameWithoutExtension;
		}
		
		relativeImportFileLocation = this.getNonExistingIncrementedFolderName(relativeImportFileLocation);
		
		createFolderOnFileSystem(relativeImportFileLocation);
		
		return relativeImportFileLocation;
	}
	
	private String storeZipFileOnTempLocation(MultipartFile file, String temporarilySystemGeneratedFileName) throws IOException {
		
		String tempFilePathAbsolute = this.tempLocation + File.separator + temporarilySystemGeneratedFileName;
		Path fileStorageLocationPath = Paths.get(tempFilePathAbsolute);
		Files.copy(file.getInputStream(), fileStorageLocationPath, StandardCopyOption.REPLACE_EXISTING);
		return tempFilePathAbsolute;
	}
	
	private String createTemporaryZipFilePath(String zipFileName) {
		
		zipFileName = TimeUtils.getCurrentDateFormatted() + CommonUtils.RESOURCE_NAME_APPENDER + zipFileName;
		return this.tempLocation + File.separator + zipFileName;
	}
	
	private String getAbsolutePathToCompress(ResourceDTO resourceDTO) {
		
		String absolutePathToCompress = this.resourcesBaseLocation;
		if(Objects.nonNull(resourceDTO.getParentResourceId())) {
			Resource parentResource = this.resourceRepository.findById(resourceDTO.getParentResourceId()).get();
			absolutePathToCompress = this.resourcesBaseLocation + File.separator + parentResource.getFilesystemPath();
		}
		
		return absolutePathToCompress;
	}
	
	private void createStackAndRemoveAllEntries(Resource parentEntity, List<Resource> entities) {
		
		entities.add(parentEntity);
		
		List<Resource> childEntities = this.resourceRepository.findAllByParentResource(parentEntity);
		
		if(Objects.nonNull(childEntities) && !childEntities.isEmpty()) {
			for(Resource entity : childEntities) {
				this.createStackAndRemoveAllEntries(entity, entities);
			}
		}
	}
}
