package com.uploader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uploader.entity.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
	
	Document findByResourceId(Long resourceId);
}
