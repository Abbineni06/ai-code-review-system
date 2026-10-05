package com.Saikiran.CodeReviewer.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String repositoryUrl;
    private String repositoryName;
    private String owner;
    private String language;
    private String framework;
    private String branch;

    private Integer score;

    private Integer securityScore;
    private Integer codeQualityScore;
    private Integer maintainabilityScore;
    private Integer complexityScore;

    private Integer filesAnalyzed;
    private Integer totalFiles;
    private Integer linesOfCode;

    private Integer totalIssues = 0;
    private Integer criticalIssues = 0;
    private Integer highIssues = 0;
    private Integer mediumIssues = 0;
    private Integer lowIssues = 0;

    private Double averageComplexity;
    private Integer highComplexityMethods;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewIssue> issues = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFramework() { return framework; }
    public void setFramework(String framework) { this.framework = framework; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getTotalIssues() { return totalIssues; }
    public void setTotalIssues(Integer totalIssues) { this.totalIssues = totalIssues; }

    public Integer getCriticalIssues() { return criticalIssues; }
    public void setCriticalIssues(Integer criticalIssues) { this.criticalIssues = criticalIssues; }

    public Integer getHighIssues() { return highIssues; }
    public void setHighIssues(Integer highIssues) { this.highIssues = highIssues; }

    public Integer getMediumIssues() { return mediumIssues; }
    public void setMediumIssues(Integer mediumIssues) { this.mediumIssues = mediumIssues; }

    public Integer getLowIssues() { return lowIssues; }
    public void setLowIssues(Integer lowIssues) { this.lowIssues = lowIssues; }

    public Integer getSecurityScore() { return securityScore; }
    public void setSecurityScore(Integer securityScore) { this.securityScore = securityScore; }

    public Integer getCodeQualityScore() { return codeQualityScore; }
    public void setCodeQualityScore(Integer codeQualityScore) { this.codeQualityScore = codeQualityScore; }

    public Integer getMaintainabilityScore() { return maintainabilityScore; }
    public void setMaintainabilityScore(Integer maintainabilityScore) { this.maintainabilityScore = maintainabilityScore; }

    public Integer getComplexityScore() { return complexityScore; }
    public void setComplexityScore(Integer complexityScore) { this.complexityScore = complexityScore; }

    public Integer getFilesAnalyzed() { return filesAnalyzed; }
    public void setFilesAnalyzed(Integer filesAnalyzed) { this.filesAnalyzed = filesAnalyzed; }

    public Integer getTotalFiles() { return totalFiles; }
    public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }

    public Integer getLinesOfCode() { return linesOfCode; }
    public void setLinesOfCode(Integer linesOfCode) { this.linesOfCode = linesOfCode; }

    public Double getAverageComplexity() { return averageComplexity; }
    public void setAverageComplexity(Double averageComplexity) { this.averageComplexity = averageComplexity; }

    public Integer getHighComplexityMethods() { return highComplexityMethods; }
    public void setHighComplexityMethods(Integer highComplexityMethods) { this.highComplexityMethods = highComplexityMethods; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<ReviewIssue> getIssues() { return issues; }
    public void setIssues(List<ReviewIssue> issues) { this.issues = issues; }
    public void addIssue(ReviewIssue issue) {
        issues.add(issue);
        issue.setReview(this);
    }
}
