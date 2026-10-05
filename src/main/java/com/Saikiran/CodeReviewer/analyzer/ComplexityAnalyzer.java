package com.Saikiran.CodeReviewer.analyzer;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ComplexityAnalyzer {

    public List<ReviewIssue> analyze(GitHubFile file) {
        List<ReviewIssue> issues = new ArrayList<>();
        String content = file.getContent();
        if (content == null || content.isEmpty()) return issues;

        String[] lines = content.split("\r?\n");
        int loopDepth = 0;
        int maxDepth = 0;
        int maxDepthLine = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("for ") || line.startsWith("for(") || line.startsWith("while ") || line.startsWith("while(") || line.contains(".forEach(")) {
                loopDepth++;
                if (loopDepth > maxDepth) {
                    maxDepth = loopDepth;
                    maxDepthLine = i + 1;
                }
            }
            if (line.contains("}") && loopDepth > 0) {
                loopDepth--;
            }
        }

        if (maxDepth >= 2) {
            ReviewIssue issue = new ReviewIssue();
            issue.setSeverity("MEDIUM");
            issue.setCategory("PERFORMANCE");
            issue.setTitle("Nested Loop / Potential O(n²) Complexity");
            issue.setDescription("Detected nested loops with depth " + maxDepth + ". This can lead to O(n²) or worse performance.");
            issue.setFile(file.getPath());
            issue.setLineNumber(maxDepthLine);
            issue.setEndLine(maxDepthLine);
            issue.setCodeSnippet("Nested loop detected around here.");
            issue.setWhyThisMatters("As dataset sizes grow, quadratic complexity will severely slow down processing and block the event loop.");
            issue.setImpact("Sluggish UI or high server CPU utilization on large inputs.");
            issue.setRecommendation("Consider refactoring using HashMaps/Sets to reduce lookup complexity to O(1) inside loops.");
            issue.setSuggestedFix("const map = new Map();\n// Pre-process into map to avoid inner loop");
            issue.setConfidence("0.95");
            issues.add(issue);
        }

        return issues;
    }
}
