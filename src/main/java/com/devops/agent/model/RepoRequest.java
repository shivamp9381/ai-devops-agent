package com.devops.agent.model;

public class RepoRequest {

    private String repoUrl;

    public RepoRequest() {}

    public RepoRequest(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
}
