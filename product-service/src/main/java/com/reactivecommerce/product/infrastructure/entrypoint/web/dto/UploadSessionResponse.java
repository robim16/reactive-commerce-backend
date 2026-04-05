package com.reactivecommerce.product.infrastructure.entrypoint.web.dto;

import java.util.UUID;

public record UploadSessionResponse(UUID assetId, String presignedUploadUrl) {}
