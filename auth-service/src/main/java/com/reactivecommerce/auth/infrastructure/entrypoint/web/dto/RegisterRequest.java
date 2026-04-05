package com.reactivecommerce.auth.infrastructure.entrypoint.web.dto;

public record RegisterRequest(String name, String email, String password, String role) {}
