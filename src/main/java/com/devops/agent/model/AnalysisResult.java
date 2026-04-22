package com.devops.agent.model;

import java.util.List;

public class AnalysisResult {

    private String stack;
    private String dockerfile;
    private String compose;
    private String env;
    private String githubActions;
    private String deploySteps;
    private String readme;

    private String repoExplanation;

    private String securityStatus;
    private int securityScore;

    private List<String> risks;
    private List<String> recommendations;

    public String getStack() {
        return stack;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    public String getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    public String getCompose() {
        return compose;
    }

    public void setCompose(String compose) {
        this.compose = compose;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getGithubActions() {
        return githubActions;
    }

    public void setGithubActions(String githubActions) {
        this.githubActions = githubActions;
    }

    public String getDeploySteps() {
        return deploySteps;
    }

    public void setDeploySteps(String deploySteps) {
        this.deploySteps = deploySteps;
    }

    public String getReadme() {
        return readme;
    }

    public void setReadme(String readme) {
        this.readme = readme;
    }

    public String getRepoExplanation() {
        return repoExplanation;
    }

    public void setRepoExplanation(String repoExplanation) {
        this.repoExplanation = repoExplanation;
    }

    public String getSecurityStatus() {
        return securityStatus;
    }

    public void setSecurityStatus(String securityStatus) {
        this.securityStatus = securityStatus;
    }

    public int getSecurityScore() {
        return securityScore;
    }

    public void setSecurityScore(int securityScore) {
        this.securityScore = securityScore;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}