package com.reactivecommerce.auth.infrastructure.entrypoint.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de registro con validación Jakarta Bean Validation.
 *
 * La validación se activa en AuthHandler mediante Mono.error()
 * cuando los campos no cumplen las restricciones.
 * Spring WebFlux no valida automáticamente los body de RouterFunction —
 * hay que invocar el Validator explícitamente (ver AuthHandler).
 */
public record RegisterRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
        String name,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
                message = "La contraseña debe contener al menos una mayúscula, una minúscula y un número"
        )
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(
                regexp = "^(BUYER|CREATOR|ADMIN|MODERATOR)$",
                message = "El rol debe ser BUYER, ADMIN, MODERATOR o CREATOR"
        )
        String role

) {}
