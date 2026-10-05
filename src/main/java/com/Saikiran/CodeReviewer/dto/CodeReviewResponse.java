package com.Saikiran.CodeReviewer.dto;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import java.util.List;

public class CodeReviewResponse {

    private String summary;
    private List<ReviewIssue> issues;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<ReviewIssue> getIssues() { return issues; }
    public void setIssues(List<ReviewIssue> issues) { this.issues = issues; }
}
