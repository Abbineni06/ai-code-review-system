package com.Saikiran.CodeReviewer.analyzer;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityAnalyzerTest {

    private final SecurityAnalyzer analyzer = new SecurityAnalyzer();

    @Test
    public void testSqlInjectionDetection() {
        GitHubFile file = new GitHubFile();
        file.setPath("UserService.java");
        file.setContent("String sql = \"SELECT * FROM users WHERE name = '\" + name + \"'\";");
        List<ReviewIssue> issues = analyzer.analyze(file);
        assertEquals(1, issues.size());
        assertEquals("Possible SQL Injection", issues.get(0).getTitle());
    }

    @Test
    public void testHardcodedPassword() {
        GitHubFile file = new GitHubFile();
        file.setPath("Config.java");
        file.setContent("String password = \"superSecret123\";");
        List<ReviewIssue> issues = analyzer.analyze(file);
        assertEquals(1, issues.size());
        assertEquals("Hardcoded Password", issues.get(0).getTitle());
    }
}
