package com.Saikiran.CodeReviewer.ai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Saikiran.CodeReviewer.dto.CodeReviewResponse;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIReviewService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AIReviewService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CodeReviewResponse generateReview(List<GitHubFile> files, String metadata) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert, senior software engineer performing a premium code review. Analyze the following repository files.\n");
        prompt.append("Metadata: ").append(metadata).append("\n\n");

        int chars = 0;
        for (GitHubFile file : files) {
            if (chars > 15000) break;
            prompt.append("File: ").append(file.getPath()).append("\n");
            prompt.append("```\n");
            String[] lines = file.getContent().split("\\r?\\n");
            for (int i = 0; i < lines.length; i++) {
                prompt.append(i + 1).append(" | ").append(lines[i]).append("\n");
            }
            prompt.append("```\n\n");
            chars += file.getContent().length();
        }

        String jsonSchema = """
            {
              "summary": "Overall human-friendly summary of the repository architecture and quality.",
              "issues": [
                {
                  "severity": "HIGH",
                  "category": "SECURITY",
                  "title": "Potential SQL Injection",
                  "description": "Short summary of the problem.",
                  "file": "path/to/file.java",
                  "lineNumber": 42,
                  "endLine": 42,
                  "codeSnippet": "String sql = \\"SELECT * FROM users WHERE name = '\\" + name + \\"'\\";",
                  "whyThisMatters": "This allows attacker-controlled input to alter the SQL query.",
                  "impact": "Can lead to complete database compromise.",
                  "recommendation": "Use PreparedStatement / parameterized query.",
                  "suggestedFix": "PreparedStatement stmt = conn.prepareStatement(\\"SELECT * FROM users WHERE name = ?\\");\\nstmt.setString(1, name);",
                  "confidence": "HIGH"
                }
              ]
            }
            """;

        prompt.append("CRITICAL INSTRUCTIONS:\n");
        prompt.append("1. For every issue found, YOU MUST EXACTLY QUOTE the code snippet from the provided file content. DO NOT invent code.\n");
        prompt.append("2. If two adjacent lines are part of the same issue, group them into ONE issue using `lineNumber` and `endLine`.\n");
        prompt.append("3. Output a single `issues` array. Valid categories: SECURITY, BUG, PERFORMANCE, CODE_QUALITY, MAINTAINABILITY, COMPLEXITY, ARCHITECTURE, BEST_PRACTICE. Valid severities: CRITICAL, HIGH, MEDIUM, LOW.\n");
        prompt.append("4. Use a senior developer tone. Explain `whyThisMatters` in simple practical terms.\n");
        prompt.append("5. DO NOT provide generic findings without specific line numbers and code snippets.\n");
        prompt.append("Return ONLY raw JSON matching this schema:\n");
        prompt.append(jsonSchema);

        try {
            String responseStr = chatClient.prompt()
                    .user(prompt.toString())
                    .call()
                    .content();

            if (responseStr.startsWith("```json")) {
                responseStr = responseStr.substring(7);
            }
            if (responseStr.startsWith("```")) {
                responseStr = responseStr.substring(3);
            }
            if (responseStr.endsWith("```")) {
                responseStr = responseStr.substring(0, responseStr.length() - 3);
            }

            return objectMapper.readValue(responseStr.trim(), CodeReviewResponse.class);
                } catch (Exception e) {
            CodeReviewResponse fallback = new CodeReviewResponse();
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("quota"))) {
                fallback.setSummary("Gemini API Rate Limit Exceeded. The AI analysis could not be completed because the API quota was exhausted. The application has safely fallen back to the local static analysis engine.");
            } else {
                fallback.setSummary("AI analysis failed or timed out. The system has automatically fallen back to the local static analysis engine to provide immediate feedback.");
            }
            return fallback;
        }
    }
}
