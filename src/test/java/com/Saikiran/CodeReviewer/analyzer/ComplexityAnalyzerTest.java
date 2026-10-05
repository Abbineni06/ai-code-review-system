package com.Saikiran.CodeReviewer.analyzer;

import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ComplexityAnalyzerTest {

    private final ComplexityAnalyzer analyzer = new ComplexityAnalyzer();

    @Test
    public void testHighComplexityDetection() {
        GitHubFile file = new GitHubFile();
        file.setPath("Logic.java");
        file.setContent("""
        public void process() {
            if (a) {
                if (b) {
                    for (int i=0; i<10; i++) {
                        if (c) {
                        } else if (d) {
                        }
                    }
                }
            }
            while (x) {
                if (y) { }
                else if (z) { }
            }
            if (foo) {
                if (bar) {
                }
            }
        }
        """);
        List<ReviewIssue> issues = analyzer.analyze(file);
        assertEquals(1, issues.size());
        assertEquals("High Cyclomatic Complexity", issues.get(0).getTitle());
    }
}
