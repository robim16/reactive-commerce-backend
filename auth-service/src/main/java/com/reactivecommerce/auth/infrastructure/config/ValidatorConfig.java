package com.reactivecommerce.auth.infrastructure.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expone el Validator de Jakarta como bean de Spring para que
 * AuthHandler pueda inyectarlo y validar los DTOs manualmente.
 *
 * En Spring WebFlux con RouterFunction los DTOs del body NO se validan
 * automáticamente con @Valid — esa anotación solo funciona en
 * @RestController + @RequestBody. En RouterFunction hay que invocar
 * el Validator explícitamente.
 *
 * spring-boot-starter-validation incluye Hibernate Validator como
 * implementación de referencia de Jakarta Bean Validation 3.0.
 */
@Configuration
public class ValidatorConfig {

    @Bean
    public Validator validator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        return factory.getValidator();
    }
}
