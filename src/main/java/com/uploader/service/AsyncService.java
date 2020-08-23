package com.uploader.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.uploader.entity.Resource;
import com.uploader.repository.ResourceRepository;
import com.uploader.utils.TimeUtils;

@Service
public class AsyncService {

	@Autowired
	private ResourceRepository resourceRepository;
	
	@Value(value = "${resources_base_location}")
	private String resourcesBaseLocation;
	
	@Async("threadPoolTaskExecutor")
	public void updateFolderSizeRecursively(Long folderId) {
		
		List<Resource> resources = new ArrayList<Resource>();
		this.buildStackForFolders(folderId, resources);
		
		for(Resource entry : resources) {
			entry.setSizeInBytes(FileUtils.sizeOfDirectory(new File(this.resourcesBaseLocation + entry.getFilesystemPath())));
			entry.setLastModifiedDateTime(TimeUtils.getCurrentUTCTimeInMilliseconds());
			this.resourceRepository.save(entry);
		}
	}
	
	private void buildStackForFolders(Long folderId, List<Resource> folders) {
		
		Resource resource = this.resourceRepository.findById(folderId).get();
		folders.add(resource);
		
		if(Objects.nonNull(resource.getParentResource())) {
			this.buildStackForFolders(resource.getParentResource().getResourceId(), folders);
		}
	}
}
