package com.Saikiran.CodeReviewer.controller;

import com.Saikiran.CodeReviewer.dto.CodeReviewRequest;
import com.Saikiran.CodeReviewer.dto.PasteCodeRequest;
import com.Saikiran.CodeReviewer.entity.Review;
import com.Saikiran.CodeReviewer.entity.ReviewIssue;
import com.Saikiran.CodeReviewer.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PostMapping
	public ResponseEntity<Review> createReview(@Valid @RequestBody CodeReviewRequest request) {
		Review review = reviewService.performReview(request.getRepositoryUrl(), java.util.Optional.ofNullable(request.getBranch()));
		return ResponseEntity.ok(review);
	}

	@PostMapping("/paste")
	public ResponseEntity<Review> createPasteReview(@Valid @RequestBody PasteCodeRequest request) {
	    Review review = reviewService.analyzePastedCode(request.getCode(), request.getLanguage());
	    return ResponseEntity.ok(review);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
	    reviewService.deleteReview(id);
	    return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<Review>> getReviews() {
		return ResponseEntity.ok(reviewService.getAllReviews());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Review> getReview(@PathVariable Long id) {
		return ResponseEntity.ok(reviewService.getReview(id));
	}

    @GetMapping("/{id}/issues")
    public ResponseEntity<List<ReviewIssue>> getReviewIssues(@PathVariable Long id) {
        Review review = reviewService.getReview(id);
        return ResponseEntity.ok(review.getIssues());
    }
}
