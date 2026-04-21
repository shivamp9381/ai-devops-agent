package com.devops.agent.model;

import java.util.List;
import java.util.Map;

public class ProjectContext {

    private String projectType;
    private List<String> fileNames;
    private Map<String, String> fileContents;

    private boolean hasDockerfile;
    private boolean hasPackageJson;
    private boolean hasPomXml;
    private boolean hasRequirementsTxt;
    private boolean hasGradleBuild;
    private boolean hasGoMod;

    private String detectedPort;
    private String startCommand;
    private String buildCommand;

    private List<String> detectedServices;

    public ProjectContext() {}

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public List<String> getFileNames() {
        return fileNames;
    }

    public void setFileNames(List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public Map<String, String> getFileContents() {
        return fileContents;
    }

    public void setFileContents(Map<String, String> fileContents) {
        this.fileContents = fileContents;
    }

    public boolean isHasDockerfile() {
        return hasDockerfile;
    }

    public void setHasDockerfile(boolean hasDockerfile) {
        this.hasDockerfile = hasDockerfile;
    }

    public boolean isHasPackageJson() {
        return hasPackageJson;
    }

    public void setHasPackageJson(boolean hasPackageJson) {
        this.hasPackageJson = hasPackageJson;
    }

    public boolean isHasPomXml() {
        return hasPomXml;
    }

    public void setHasPomXml(boolean hasPomXml) {
        this.hasPomXml = hasPomXml;
    }

    public boolean isHasRequirementsTxt() {
        return hasRequirementsTxt;
    }

    public void setHasRequirementsTxt(boolean hasRequirementsTxt) {
        this.hasRequirementsTxt = hasRequirementsTxt;
    }

    public boolean isHasGradleBuild() {
        return hasGradleBuild;
    }

    public void setHasGradleBuild(boolean hasGradleBuild) {
        this.hasGradleBuild = hasGradleBuild;
    }

    public boolean isHasGoMod() {
        return hasGoMod;
    }

    public void setHasGoMod(boolean hasGoMod) {
        this.hasGoMod = hasGoMod;
    }

    public String getDetectedPort() {
        return detectedPort;
    }

    public void setDetectedPort(String detectedPort) {
        this.detectedPort = detectedPort;
    }

    public String getStartCommand() {
        return startCommand;
    }

    public void setStartCommand(String startCommand) {
        this.startCommand = startCommand;
    }

    public String getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(String buildCommand) {
        this.buildCommand = buildCommand;
    }

    public List<String> getDetectedServices() {
        return detectedServices;
    }

    public void setDetectedServices(List<String> detectedServices) {
        this.detectedServices = detectedServices;
    }
}