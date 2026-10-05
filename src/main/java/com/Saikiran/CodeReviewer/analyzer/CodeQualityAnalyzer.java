package com.Saikiran.CodeReviewer.analyzer;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CodeQualityAnalyzer {

    public List<ReviewIssue> analyze(GitHubFile file) {
        List<ReviewIssue> issues = new ArrayList<>();
        String content = file.getContent();
        if (content == null || content.isEmpty()) return issues;

        String[] lines = content.split("\r?\n");
        boolean inAsyncFunc = false;
        boolean hasTry = false;
        int asyncFuncLine = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.contains("async function") || line.contains("async () =>") || line.matches(".*async\\s+[a-zA-Z0-9_]+\\s*\\(.*")) {

                if (inAsyncFunc && !hasTry) {
                    issues.add(createIssue("LOW", "CODE_QUALITY", "Missing Error Handling", 
                        "Async function does not contain a try/catch block.", file.getPath(), asyncFuncLine, "async function...",
                        "Network requests or async operations can fail. Unhandled promise rejections can crash the application or leave the UI in an inconsistent state.",
                        "Application crash or silent failures.",
                        "Wrap await calls in a try/catch block and handle errors appropriately.",
                        "try {\n  const res = await fetch(...);\n} catch (error) {\n  console.error(error);\n}"));
                }
                inAsyncFunc = true;
                hasTry = false;
                asyncFuncLine = i + 1;
            }

            if (inAsyncFunc && line.contains("try {") || line.contains("try{") || line.contains(".catch(")) {
                hasTry = true;
            }

            if (inAsyncFunc && line.startsWith("}")) { 
                if (!hasTry) {

                    issues.add(createIssue("LOW", "CODE_QUALITY", "Missing Error Handling", 
                        "Async function does not contain a try/catch block.", file.getPath(), asyncFuncLine, "async function...",
                        "Network requests or async operations can fail. Unhandled promise rejections can crash the application.",
                        "Application crash or silent failures.",
                        "Wrap await calls in a try/catch block.",
                        "try {\n  const res = await fetch(...);\n} catch (error) {\n  console.error(error);\n}"));
                }
                inAsyncFunc = false;
                hasTry = false;
            }

            if (line.contains("localStorage.setItem") && i < lines.length - 1 && lines[i+1].contains("localStorage.getItem")) {
                issues.add(createIssue("LOW", "CODE_QUALITY", "Redundant Code", 
                    "localStorage.getItem called immediately after setItem.", file.getPath(), i + 1, line.trim(),
                    "The value is already available in memory, reading it back from localStorage is unnecessary and slow.",
                    "Minor performance degradation.",
                    "Use the local variable instead of fetching from storage again.",
                    ""));
            }
        }

        if (inAsyncFunc && !hasTry) {
            issues.add(createIssue("LOW", "CODE_QUALITY", "Missing Error Handling", 
                "Async function does not contain a try/catch block.", file.getPath(), asyncFuncLine, "async function...",
                "Network requests or async operations can fail.",
                "Application crash or silent failures.",
                "Wrap await calls in a try/catch block.",
                "try {\n  const res = await fetch(...);\n} catch (error) {\n  console.error(error);\n}"));
        }

        return issues;
    }

    private ReviewIssue createIssue(String severity, String category, String title, String desc, String path, int line, String snippet, String whyThisMatters, String impact, String rec, String fix) {
        ReviewIssue issue = new ReviewIssue();
        issue.setSeverity(severity);
        issue.setCategory(category);
        issue.setTitle(title);
        issue.setDescription(desc);
        issue.setFile(path);
        issue.setLineNumber(line);
        issue.setEndLine(line);
        issue.setCodeSnippet(snippet);
        issue.setWhyThisMatters(whyThisMatters);
        issue.setImpact(impact);
        issue.setRecommendation(rec);
        issue.setSuggestedFix(fix);
        issue.setConfidence("0.90");
        return issue;
    }
}
