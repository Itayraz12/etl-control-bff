package com.example.model;

public class Deployment {
    private String id;
    private String productType;
    private String productSource;
    private String deploymentStatus;
    private String savedVersion;
    private String deployedVersion;
    private Long lastStatusChange;
    private Long createdAt;
    private String environment;

    public Deployment() {}

    public Deployment(String id, String productType, String productSource, String deploymentStatus,
                      String savedVersion, String deployedVersion, Long lastStatusChange, Long createdAt,
                      String environment) {
        this.id = id;
        this.productType = productType;
        this.productSource = productSource;
        this.deploymentStatus = deploymentStatus;
        this.savedVersion = savedVersion;
        this.deployedVersion = deployedVersion;
        this.lastStatusChange = lastStatusChange;
        this.createdAt = createdAt;
        this.environment = environment;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductSource() { return productSource; }
    public void setProductSource(String productSource) { this.productSource = productSource; }

    public String getDeploymentStatus() { return deploymentStatus; }
    public void setDeploymentStatus(String deploymentStatus) { this.deploymentStatus = deploymentStatus; }

    public String getSavedVersion() { return savedVersion; }
    public void setSavedVersion(String savedVersion) { this.savedVersion = savedVersion; }

    public String getDeployedVersion() { return deployedVersion; }
    public void setDeployedVersion(String deployedVersion) { this.deployedVersion = deployedVersion; }

    public Long getLastStatusChange() { return lastStatusChange; }
    public void setLastStatusChange(Long lastStatusChange) { this.lastStatusChange = lastStatusChange; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    @Override
    public String toString() {
        return "Deployment{" +
                "id='" + id + '\'' +
                ", productType='" + productType + '\'' +
                ", productSource='" + productSource + '\'' +
                ", deploymentStatus='" + deploymentStatus + '\'' +
                ", savedVersion='" + savedVersion + '\'' +
                ", deployedVersion='" + deployedVersion + '\'' +
                ", lastStatusChange=" + lastStatusChange +
                ", createdAt=" + createdAt +
                ", environment='" + environment + '\'' +
                '}';
    }
}

