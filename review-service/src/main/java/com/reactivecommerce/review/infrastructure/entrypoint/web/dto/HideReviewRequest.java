package com.reactivecommerce.review.infrastructure.entrypoint.web.dto;
import jakarta.validation.constraints.NotBlank;

public record HideReviewRequest(
        @NotBlank(message = "the reason is required")
        String reason
) {}
