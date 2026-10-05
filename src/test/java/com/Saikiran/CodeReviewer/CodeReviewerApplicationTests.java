package com.Saikiran.CodeReviewer;

import com.Saikiran.CodeReviewer.dto.CodeReviewRequest;
import com.Saikiran.CodeReviewer.entity.Review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class CodeReviewerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void testFullReviewFlow() {

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("sk-fake")) {
            System.out.println("Skipping real API test because valid GEMINI_API_KEY is not set in env.");
            return;
        }

        CodeReviewRequest request = new CodeReviewRequest("https://github.com/spring-guides/gs-rest-service");
        ResponseEntity<Review> response = restTemplate.postForEntity("/api/reviews", request, Review.class);

        System.out.println("Response Status: " + response.getStatusCode());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        Review review = response.getBody();
        assertThat(review).isNotNull();
        System.out.println("Repository: " + review.getRepositoryName());
        System.out.println("Test successful! AI Summary: " + review.getSummary());
}
}
