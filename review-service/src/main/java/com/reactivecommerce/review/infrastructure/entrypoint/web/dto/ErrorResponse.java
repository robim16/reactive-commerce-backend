package com.reactivecommerce.review.infrastructure.entrypoint.web.dto;
import java.time.Instant;
public record ErrorResponse(String message, Instant timestamp) {
    public static ErrorResponse of(String m) { return new ErrorResponse(m, Instant.now()); }
}
