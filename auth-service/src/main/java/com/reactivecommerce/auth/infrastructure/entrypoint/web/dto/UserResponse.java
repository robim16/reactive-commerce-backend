package com.reactivecommerce.auth.infrastructure.entrypoint.web.dto;

import com.reactivecommerce.auth.domain.model.UserRole;
import java.util.UUID;

public record UserResponse(UUID id, String name, String email, UserRole role) {}
