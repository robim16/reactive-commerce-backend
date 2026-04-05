package com.reactivecommerce.review.infrastructure.entrypoint.web.dto;
public record CreateReviewRequest(String assetId, int rating, String comment) {}
