package com.reactivecommerce.product.infrastructure.entrypoint.web.dto;

public record MessageResponse(String message) {
    public static MessageResponse of(String m) { return new MessageResponse(m); }
}
