package com.Saikiran.CodeReviewer.analyzer;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SecurityAnalyzer {

    public List<ReviewIssue> analyze(GitHubFile file) {
        List<ReviewIssue> issues = new ArrayList<>();
        String content = file.getContent();
        if (content == null || content.isEmpty()) return issues;

        String[] lines = content.split("\r?\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lower = line.toLowerCase();

            if ((lower.contains("select ") || lower.contains("insert ") || lower.contains("update ") || lower.contains("delete ")) && (line.contains(" + ") || line.contains("${"))) {
                if (!lower.contains("preparedstatement")) {
                    issues.add(createIssue("CRITICAL", "SECURITY", "SQL Injection", 
                        "User-controlled input is directly concatenated into an SQL query.", file.getPath(), i + 1, line.trim(),
                        "This allows an attacker to inject arbitrary SQL logic, bypass authentication, or dump the entire database.",
                        "Complete database compromise.",
                        "Use parameterized queries/prepared statements.",
                        "const query = \"SELECT * FROM users WHERE id = ?\"; db.query(query, [userId]);"));
                }
            }

            if (line.matches("(?i).*(password|secret|api_?key|token)\\s*[:=]\\s*['\"][^'\"]{4,}['\"].*")) {
                if (!lower.contains("process.env") && !lower.contains("system.getenv") && !line.contains("document.getelementbyid")) {
                    issues.add(createIssue("HIGH", "SECURITY", "Hardcoded Credential", 
                        "A potential credential or secret is hardcoded in the source code.", file.getPath(), i + 1, line.trim(),
                        "Hardcoded credentials can be extracted by anyone with access to the source code or binary.",
                        "Credential leak / Unauthorized access.",
                        "Use environment variables or a secure secret manager.",
                        "const password = process.env.DB_PASSWORD;"));
                }
            }

            if (line.contains(".innerHTML") && (line.contains("=") || line.contains("+="))) {
                boolean hasConcat = line.contains("+") || line.contains("${");
                for (int j = 1; j <= 3 && (i + j) < lines.length && !hasConcat; j++) {
                    if (lines[i+j].contains("+") || lines[i+j].contains("${")) hasConcat = true;
                    if (lines[i+j].contains(";")) break;
                }
                if (hasConcat) {
                    issues.add(createIssue("HIGH", "SECURITY", "Stored/Reflected Cross-Site Scripting", 
                        "Unsafe assignment to innerHTML using concatenated strings.", file.getPath(), i + 1, line.trim(),
                        "Injecting unsanitized user input into the DOM allows attackers to execute malicious JavaScript.",
                        "Client-side code execution, session hijacking.",
                        "Use textContent/createElement or proper HTML escaping.",
                        "element.textContent = userInput;"));
                }
            }
            if ((line.contains("document.write(") || line.contains(".insertAdjacentHTML(")) && (line.contains("+") || line.contains("${"))) {
                issues.add(createIssue("HIGH", "SECURITY", "Stored/Reflected Cross-Site Scripting", 
                    "Unsafe DOM manipulation.", file.getPath(), i + 1, line.trim(),
                    "Injecting unsanitized user input into the DOM allows attackers to execute malicious JavaScript.",
                    "Client-side code execution.",
                    "Use DOM manipulation safely without parsing raw HTML strings.",
                    ""));
            }

            if (line.contains("localStorage.setItem") && (lower.contains("role") || lower.contains("auth") || lower.contains("token") || lower.contains("authenticated"))) {
                issues.add(createIssue("CRITICAL", "SECURITY", "Client-Side Authentication/Authorization Bypass", 
                    "Application state or authorization rules are stored in client-side storage (localStorage).", file.getPath(), i + 1, line.trim(),
                    "Client-side storage can be easily modified by the user using browser dev tools.",
                    "Users can elevate their own privileges or bypass authentication.",
                    "Perform authentication and authorization on the backend. Never trust client-controlled localStorage values.",
                    "// Use HttpOnly secure cookies and validate sessions server-side."));
            }
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
        issue.setConfidence("1.0");
        return issue;
    }
}
