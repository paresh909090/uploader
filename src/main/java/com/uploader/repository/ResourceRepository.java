package com.uploader.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uploader.entity.Resource;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
	
	public List<Resource> findAllByParentResourceIsNull();
	
	public List<Resource> findAllByParentResource(Resource parentResource);
	
	public Resource findByFilesystemPath(String fileSystemPath);
}
