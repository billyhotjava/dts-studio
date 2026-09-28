package com.yuzhi.dts.copilot.ai.web.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for natural language to SQL conversion.
 */
public record Nl2SqlRequest(
        @NotBlank String naturalLanguage,
        Long dataSourceId,
        String schemaContext
) {
}
