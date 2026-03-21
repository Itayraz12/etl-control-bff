package com.example.model;

public class DeployResponse {
    private boolean success;
    private String id;
    private String deploymentId;

    public DeployResponse() {}

    public DeployResponse(boolean success, String id, String deploymentId) {
        this.success = success;
        this.id = id;
        this.deploymentId = deploymentId;
    }

    public boolean isSuccess()                        { return success; }
    public void setSuccess(boolean success)           { this.success = success; }

    public String getId()                             { return id; }
    public void setId(String id)                      { this.id = id; }

    public String getDeploymentId()                   { return deploymentId; }
    public void setDeploymentId(String deploymentId)  { this.deploymentId = deploymentId; }
}

