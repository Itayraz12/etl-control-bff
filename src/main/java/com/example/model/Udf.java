package com.example.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class Udf {


    private String id;

    private String name;


    private UdfType type;

    private String description;

    @JsonProperty("isActive")
    private Boolean isActive = true;

    @JsonProperty("isApproved")
    private Boolean isApproved = false;

    private String version;

    private String filePath;

    private String team;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime dateApproved;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime updatedAt;



    // Constructors
    public Udf() {}

    public Udf(String id, String name, UdfType type, String description, Boolean isActive, 
               Boolean isApproved, String version, String filePath, String team, 
               LocalDateTime dateApproved, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.isActive = isActive;
        this.isApproved = isApproved;
        this.version = version;
        this.filePath = filePath;
        this.team = team;
        this.dateApproved = dateApproved;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public UdfType getType() { return type; }
    public String getDescription() { return description; }
    public Boolean isActive() { return isActive; }
    public Boolean isApproved() { return isApproved; }
    public String getVersion() { return version; }
    public String getFilePath() { return filePath; }
    public String getTeam() { return team; }
    public LocalDateTime getDateApproved() { return dateApproved; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(UdfType type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
    public void setVersion(String version) { this.version = version; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setTeam(String team) { this.team = team; }
    public void setDateApproved(LocalDateTime dateApproved) { this.dateApproved = dateApproved; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public enum UdfType {
        transformer,
        filter
    }
}
