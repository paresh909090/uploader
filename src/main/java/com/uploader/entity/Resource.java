package com.uploader.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.uploader.utils.ProjectEnums.ResourceType;

import lombok.Data;

@Entity
@Data
@Table(name = "resources")
public class Resource implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "original_name", length = 250)
    private String originalName;

    @Column(name = "saved_name", length = 250)
    private String savedName;
    
    @Column(name = "filesystem_path", length = 2048)
    private String filesystemPath;

    @Enumerated(EnumType.STRING)
    private ResourceType type;

    @Column(name = "create_date_time")
    private Long createDateTime;
    
    @Column(name = "size_in_bytes")
    private Long sizeInBytes;
    
    @Column(name = "last_modified_date_time")
    private Long lastModifiedDateTime;
    
    @OneToOne
    @JoinColumn(name = "parent_resource_id")
    private Resource parentResource;
}