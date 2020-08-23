package com.uploader.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uploader.entity.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
	
	Task findByResourceId(Long resourceId);
}
