package com.Saikiran.CodeReviewer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CodeReviewRequest {

    @NotBlank(message = "Repository URL cannot be blank")
    @Pattern(regexp = "^https://github\\.com/[\\w.-]+/[\\w.-]+/?$", message = "Must be a valid GitHub repository URL")
    private String repositoryUrl;

    private String branch;

    public CodeReviewRequest() {
    }

    public CodeReviewRequest(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}