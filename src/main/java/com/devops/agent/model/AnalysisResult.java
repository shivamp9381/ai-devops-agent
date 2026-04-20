package com.devops.agent.model;

import java.util.List;

public class AnalysisResult {

    private String stack;
    private String dockerfile;
    private String compose;
    private String env;
    private String githubActions;
    private String deploySteps;
    private List<String> recommendations;

    public AnalysisResult() {}

    public String getStack() { return stack; }
    public void setStack(String stack) { this.stack = stack; }

    public String getDockerfile() { return dockerfile; }
    public void setDockerfile(String dockerfile) { this.dockerfile = dockerfile; }

    public String getCompose() { return compose; }
    public void setCompose(String compose) { this.compose = compose; }

    public String getEnv() { return env; }
    public void setEnv(String env) { this.env = env; }

    public String getGithubActions() { return githubActions; }
    public void setGithubActions(String githubActions) { this.githubActions = githubActions; }

    public String getDeploySteps() { return deploySteps; }
    public void setDeploySteps(String deploySteps) { this.deploySteps = deploySteps; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
