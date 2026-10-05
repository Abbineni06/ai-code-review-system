package com.Saikiran.CodeReviewer.analyzer;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RepositoryAnalyzer {

    private final SecurityAnalyzer securityAnalyzer;
    private final CodeQualityAnalyzer codeQualityAnalyzer;
    private final ComplexityAnalyzer complexityAnalyzer;

    public RepositoryAnalyzer(SecurityAnalyzer securityAnalyzer, CodeQualityAnalyzer codeQualityAnalyzer, ComplexityAnalyzer complexityAnalyzer) {
        this.securityAnalyzer = securityAnalyzer;
        this.codeQualityAnalyzer = codeQualityAnalyzer;
        this.complexityAnalyzer = complexityAnalyzer;
    }

    public List<ReviewIssue> analyze(List<GitHubFile> files) {
        List<ReviewIssue> allIssues = new ArrayList<>();
        for (GitHubFile file : files) {
            allIssues.addAll(securityAnalyzer.analyze(file));
            allIssues.addAll(codeQualityAnalyzer.analyze(file));
            allIssues.addAll(complexityAnalyzer.analyze(file));
        }
        return allIssues;
    }
}
