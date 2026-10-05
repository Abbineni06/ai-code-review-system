package com.Saikiran.CodeReviewer.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GitHubService {

    private final RestTemplate restTemplate;

    @Value("${GITHUB_TOKEN:}")
    private String githubToken;

    private static final String GITHUB_API_URL = "https://api.github.com";

    private static final int MAX_FILES_TO_ANALYZE = 15;
    private static final long MAX_FILE_SIZE_BYTES = 50_000; 

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", "target", "node_modules", "dist", "build", "out", "bin", ".idea", ".vscode", "coverage", "test", "tests"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "java", "py", "js", "ts", "jsx", "tsx", "c", "cpp", "cs", "go", "php", "rb", "sql"
    );

    public GitHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private HttpEntity<?> createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github.v3+json");
        if (githubToken != null && !githubToken.trim().isEmpty()) {
            headers.set("Authorization", "token " + githubToken.trim());
        }
        return new HttpEntity<>(headers);
    }

    public GitHubRepoMetadata getRepositoryMetadata(String owner, String repo) {
        String url = String.format("%s/repos/%s/%s", GITHUB_API_URL, owner, repo);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, createHeaders(), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Empty response from GitHub");

            GitHubRepoMetadata metadata = new GitHubRepoMetadata();
            metadata.setName((String) body.get("name"));
            metadata.setOwner((String) ((Map<?, ?>) body.get("owner")).get("login"));
            metadata.setDescription((String) body.get("description"));
            metadata.setDefaultBranch((String) body.get("default_branch"));
            metadata.setLanguage((String) body.get("language"));
            return metadata;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN && e.getResponseBodyAsString().contains("rate limit")) {
                throw new RuntimeException("GitHub API rate limit reached. Please configure a GitHub token.");
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RuntimeException("GitHub repository not found or is private.");
            }
            throw new RuntimeException("GitHub API error: " + e.getMessage());
        }
    }

    private int getFilePriority(String path) {
        String lowerPath = path.toLowerCase();

        if (lowerPath.contains("service") || lowerPath.contains("controller") || lowerPath.contains("repository") || lowerPath.contains("config")) {
            return 1;
        }

        if (lowerPath.endsWith(".java") || lowerPath.endsWith(".py") || lowerPath.endsWith(".js") || lowerPath.endsWith(".ts")) {
            if (lowerPath.contains("test")) return 4; 
            return 2;
        }

        if (lowerPath.endsWith(".xml") || lowerPath.endsWith(".properties") || lowerPath.endsWith(".yml") || lowerPath.endsWith(".json")) {
            return 3;
        }

        if (lowerPath.contains("test")) {
            return 4;
        }

        if (lowerPath.endsWith(".md") || lowerPath.endsWith(".txt")) {
            return 5;
        }
        return 6;
    }

    public List<GitHubFile> getRepositoryFiles(String owner, String repo, String branch) {

        String url = String.format("%s/repos/%s/%s/git/trees/%s?recursive=1", GITHUB_API_URL, owner, repo, branch);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, createHeaders(), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("tree")) return Collections.emptyList();

            List<Map<String, Object>> tree = (List<Map<String, Object>>) body.get("tree");

            List<Map<String, Object>> validFiles = tree.stream()
                    .filter(item -> "blob".equals(item.get("type")))
                    .filter(item -> isValidFile((String) item.get("path")))
                    .filter(item -> {
                        Object sizeObj = item.get("size");
                        if (sizeObj instanceof Number) {
                            return ((Number) sizeObj).longValue() <= MAX_FILE_SIZE_BYTES;
                        }
                        return true;
                    })
                    .sorted((f1, f2) -> {
                        String p1 = (String) f1.get("path");
                        String p2 = (String) f2.get("path");
                        return Integer.compare(getFilePriority(p1), getFilePriority(p2));
                    })
                    .limit(30)
                    .collect(Collectors.toList());

            return validFiles.parallelStream()
                    .map(item -> {
                        String path = (String) item.get("path");
                        String content = getFileContent(owner, repo, branch, path);
                        if (content != null) {
                            GitHubFile file = new GitHubFile();
                            file.setPath(path);
                            file.setContent(content);
                            return file;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to fetch repository tree: " + e.getMessage());
        }
    }

    private String getFileContent(String owner, String repo, String branch, String path) {
        String url = String.format("%s/repos/%s/%s/contents/%s?ref=%s", GITHUB_API_URL, owner, repo, path, branch);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, createHeaders(), Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && "base64".equals(body.get("encoding"))) {
                String base64Content = (String) body.get("content");
                if (base64Content != null) {
                    return new String(Base64.getMimeDecoder().decode(base64Content));
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to fetch file content for " + path + ": " + e.getMessage());
            return null;
        }
    }

    private boolean isValidFile(String path) {
        String[] parts = path.split("/");
        for (String part : parts) {
            if (IGNORED_DIRECTORIES.contains(part)) return false;
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot > 0 && lastDot < path.length() - 1) {
            String ext = path.substring(lastDot + 1).toLowerCase();
            return ALLOWED_EXTENSIONS.contains(ext);
        }
        return false;
    }
}
