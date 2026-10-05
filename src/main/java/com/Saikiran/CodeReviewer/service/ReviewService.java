package com.Saikiran.CodeReviewer.service;

import com.Saikiran.CodeReviewer.ai.AIReviewService;
import com.Saikiran.CodeReviewer.analyzer.RepositoryAnalyzer;
import com.Saikiran.CodeReviewer.dto.CodeReviewResponse;
import com.Saikiran.CodeReviewer.entity.Review;
import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.github.GitHubFile;
import com.Saikiran.CodeReviewer.github.GitHubRepoMetadata;
import com.Saikiran.CodeReviewer.github.GitHubService;
import com.Saikiran.CodeReviewer.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final GitHubService gitHubService;
    private final RepositoryAnalyzer repositoryAnalyzer;
    private final AIReviewService aiReviewService;
    private final ReviewRepository reviewRepository;

    public ReviewService(GitHubService gitHubService,
                         RepositoryAnalyzer repositoryAnalyzer,
                         AIReviewService aiReviewService,
                         ReviewRepository reviewRepository) {
        this.gitHubService = gitHubService;
        this.repositoryAnalyzer = repositoryAnalyzer;
        this.aiReviewService = aiReviewService;
        this.reviewRepository = reviewRepository;
    }

    public Review performReview(String repositoryUrl, Optional<String> branchOpt) {
        long startTime = System.currentTimeMillis();

        String[] parts = repositoryUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL");
        }
        String owner = parts[0];
        String repo = parts[1];

        GitHubRepoMetadata metadata = gitHubService.getRepositoryMetadata(owner, repo);
        String targetBranch = (branchOpt != null && branchOpt.isPresent() && !branchOpt.get().trim().isEmpty()) ? branchOpt.get().trim() : metadata.getDefaultBranch();

        List<GitHubFile> files = gitHubService.getRepositoryFiles(owner, repo, targetBranch);

        if (files.isEmpty()) {
            throw new RuntimeException("Repository is empty or no valid source files found.");
        }

        String metaString = String.format("Repo: %s, Owner: %s, Description: %s", metadata.getName(), metadata.getOwner(), metadata.getDescription());

        Review review = executePipeline(repositoryUrl, metadata.getName(), metadata.getOwner(), metadata.getLanguage(), metadata.getDefaultBranch(), files, metaString);

        return reviewRepository.save(review);
    }

        public Review analyzePastedCode(String code, String language) {
        long startTime = System.currentTimeMillis();
        String detectedLang = language != null ? language : detectLanguageFromCode(code);
        String ext = "java"; 
        switch(detectedLang.toLowerCase()) {
            case "python": ext = "py"; break;
            case "javascript": ext = "js"; break;
            case "typescript": ext = "ts"; break;
            case "html": ext = "html"; break;
            case "css": ext = "css"; break;
            case "sql": ext = "sql"; break;
        }

        GitHubFile file = new GitHubFile();
        file.setPath("PastedCode." + ext);
        file.setContent(code);

        Review review = executePipeline("Pasted Code", "Pasted Snippet", "Local", detectedLang, "main", List.of(file), "Pasted code snippet analysis.");
        return reviewRepository.save(review);
    }

    private String detectLanguageFromCode(String code) {
        if (code == null) return "Unknown";
        String lower = code.toLowerCase();
        if (code.contains("public class") || code.contains("public static void main") || code.contains("System.out.println") || code.contains("import java.")) return "Java";
        if (code.contains("const ") || code.contains("let ") || code.contains("document.") || code.contains("console.log") || code.contains("localStorage") || code.contains("=>")) return "JavaScript";
        if (code.contains("def ") || code.contains("import ") && code.contains("print(") || code.contains("self.")) return "Python";
        if (code.contains("interface ") && code.contains("type ")) return "TypeScript";
        if (code.contains("<html>") || code.contains("</div>")) return "HTML";
        if (code.contains("SELECT ") && code.contains("FROM ")) return "SQL";
        return "Unknown";
    }

    private Review executePipeline(String repoUrl, String repoName, String owner, String lang, String branch, List<GitHubFile> files, String metaString) {
        List<ReviewIssue> staticIssues = new ArrayList<>();

        CodeReviewResponse aiResponse = new CodeReviewResponse();
        try {
            aiResponse = aiReviewService.generateReview(files, metaString);
        } catch (Exception e) {
            System.err.println("AI Review Failed: " + e.getMessage());
        }

        if (aiResponse.getIssues() == null || aiResponse.getIssues().isEmpty()) {
            aiResponse.setIssues(new java.util.ArrayList<>());
            if (aiResponse.getSummary() == null) {
                aiResponse.setSummary("⚠️ AI analysis is currently unavailable due to high API demand (Gemini rate limits). Showing Static Analysis results only.");
            }
        }

        staticIssues = repositoryAnalyzer.analyze(files);

        List<ReviewIssue> validatedAiIssues = validateFindings(aiResponse.getIssues(), files);

        List<ReviewIssue> combinedIssues = deduplicate(staticIssues, validatedAiIssues);

        Review review = new Review();
        review.setRepositoryUrl(repoUrl);
        review.setRepositoryName(repoName);
        review.setOwner(owner);
        review.setLanguage(lang);
        review.setBranch(branch);

        String detectedFramework = "None";
        boolean hasSpring = false;
        boolean hasReact = false;
        boolean hasAngular = false;
        boolean hasVue = false;
        boolean hasDjango = false;
        boolean hasExpress = false;

        int javaCount = 0;
        int jsCount = 0;
        int pyCount = 0;

        for (GitHubFile f : files) {
            String path = f.getPath().toLowerCase();
            String content = f.getContent() != null ? f.getContent().toLowerCase() : "";

            if (path.endsWith(".java")) javaCount++;
            if (path.endsWith(".js") || path.endsWith(".ts") || path.endsWith(".jsx") || path.endsWith(".tsx")) jsCount++;
            if (path.endsWith(".py")) pyCount++;

            if (content.contains("org.springframework")) hasSpring = true;
            if (content.contains("react") || content.contains("import react")) hasReact = true;
            if (content.contains("@angular/core")) hasAngular = true;
            if (content.contains("vue") || path.endsWith(".vue")) hasVue = true;
            if (content.contains("django")) hasDjango = true;
            if (content.contains("express") && content.contains("require('express')")) hasExpress = true;
        }

        if (hasSpring) detectedFramework = "Spring Boot";
        else if (hasReact) detectedFramework = "React";
        else if (hasAngular) detectedFramework = "Angular";
        else if (hasVue) detectedFramework = "Vue.js";
        else if (hasDjango) detectedFramework = "Django";
        else if (hasExpress) detectedFramework = "Express.js";
        else if (jsCount > javaCount && jsCount > pyCount) detectedFramework = "Vanilla JS";

        if ("CSS".equalsIgnoreCase(lang) || "HTML".equalsIgnoreCase(lang)) {
            if (jsCount > javaCount && jsCount > pyCount && jsCount > 0) lang = "JavaScript";
            if (javaCount > jsCount && javaCount > pyCount && javaCount > 0) lang = "Java";
            if (pyCount > jsCount && pyCount > javaCount && pyCount > 0) lang = "Python";
        }

        review.setFramework(detectedFramework);

        review.setFilesAnalyzed(files.size());

        review.setTotalFiles(files.size()); 

        int totalLines = 0;
        for (GitHubFile f : files) {
            if (f.getContent() != null) {
                totalLines += f.getContent().split("\\r?\\n").length;
            }
        }
        review.setLinesOfCode(totalLines);
        review.setSummary(aiResponse.getSummary() != null ? aiResponse.getSummary() : "Analysis completed.");

        int score = 100;
        int critical = 0, high = 0, med = 0, low = 0;
        double totalComplexity = 0.0;
        int complexityCount = 0;
        int highComplexityMethods = 0;

        for (ReviewIssue issue : combinedIssues) {
            if (issue.getSeverity() != null) {
                switch (issue.getSeverity().toUpperCase()) {
                    case "CRITICAL": critical++; score -= 15; break;
                    case "HIGH": high++; score -= 8; break;
                    case "MEDIUM": med++; score -= 3; break;
                    case "LOW": low++; score -= 1; break;
                    default: low++; score -= 1; break;
                }
            }

            if ("COMPLEXITY".equalsIgnoreCase(issue.getCategory())) {
                complexityCount++;
                if ("HIGH".equalsIgnoreCase(issue.getSeverity())) {
                    highComplexityMethods++;
                }

                totalComplexity += 15.0; 
            }
        }

        review.setScore(Math.max(10, score));
        if (combinedIssues.isEmpty() && !files.isEmpty()) {
            review.setScore(100);
        }

        review.setSecurityScore(Math.max(10, 100 - (critical * 15 + high * 10)));
        review.setCodeQualityScore(Math.max(10, 100 - (med * 5 + low * 2)));
        review.setMaintainabilityScore(Math.max(10, 100 - (complexityCount * 3)));
        review.setComplexityScore(Math.max(10, 100 - (highComplexityMethods * 10)));

        review.setTotalIssues(combinedIssues.size());
        review.setCriticalIssues(critical);
        review.setHighIssues(high);
        review.setMediumIssues(med);
        review.setLowIssues(low);
        review.setAverageComplexity(complexityCount > 0 ? (totalComplexity / complexityCount) : 1.0);
        review.setHighComplexityMethods(highComplexityMethods);

        for (ReviewIssue issue : combinedIssues) {
            issue.setReview(review);
        }
        review.setIssues(combinedIssues);

        return review;
    }

    private List<ReviewIssue> validateFindings(List<ReviewIssue> aiIssues, List<GitHubFile> files) {
        if (aiIssues == null) return new ArrayList<>();
        return aiIssues.stream()
                .filter(issue -> issue.getFile() != null && !issue.getFile().isEmpty())
                .filter(issue -> isValidLine(issue, files))
                .collect(Collectors.toList());
    }

    private boolean isValidLine(ReviewIssue issue, List<GitHubFile> files) {
        if (issue.getLineNumber() == null || issue.getLineNumber() <= 0) {
            issue.setLineNumber(1);
        }
        if (issue.getEndLine() == null || issue.getEndLine() < issue.getLineNumber()) {
            issue.setEndLine(issue.getLineNumber());
        }

        for (GitHubFile file : files) {
            if (file.getPath().equals(issue.getFile())) {
                String[] lines = file.getContent().split("\\r?\\n");
                if (issue.getLineNumber() > lines.length) {
                    issue.setLineNumber(lines.length);
                    issue.setEndLine(lines.length);
                }
                return true;
            }
        }

        return true;
    }

    private List<ReviewIssue> deduplicate(List<ReviewIssue> staticIssues, List<ReviewIssue> aiIssues) {
        List<ReviewIssue> combined = new ArrayList<>(staticIssues);
        for (ReviewIssue ai : aiIssues) {
            boolean isDuplicate = false;
            for (ReviewIssue st : staticIssues) {
                if (Objects.equals(st.getFile(), ai.getFile()) && 
                    Objects.equals(st.getLineNumber(), ai.getLineNumber()) &&
                    Objects.equals(st.getCategory(), ai.getCategory())) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                combined.add(ai);
            }
        }
        return combined;
    }

    public Review getReview(Long id) {
        return reviewRepository.findById(id).orElseThrow(() -> new RuntimeException("Review not found"));
    }

    public void deleteReview(Long id) {
        reviewRepository.deleteById(id);
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }
}
