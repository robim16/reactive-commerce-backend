package com.reactivecommerce.product.infrastructure.entrypoint.web.dto;

import java.util.List;

public record CreateAssetRequest(String title, String description, String category,
                                  List<String> tags, String price, String license,
                                  String format, Long fileSizeBytes) {}
