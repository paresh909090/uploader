package com.uploader.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ResourceDTO {
	
	private Long resourceId;
	private MultipartFile file;
	private String resourceOriginalName;
	private Long lastAccessedDateTime;
	private Long createDateTime;
	private Long sizeInBytes;
	private String resourceType;
	private Long parentResourceId;
	private String parentResourceName;
	private String savedName;
	private String fileSystemPath;
}
