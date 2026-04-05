package com.reactivecommerce.product.infrastructure.entrypoint.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de request para PATCH/PUT /{id}.
 * Todos los campos son opcionales (null = no cambiar).
 * Se usa para actualizar metadatos de un asset ya creado (HU-PRO-03).
 */
public record UpdateAssetRequest(
    String      title,
    String      description,
    BigDecimal  price,
    List<String> tags,
    String      license
) {}
