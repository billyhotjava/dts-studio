package com.yuzhi.dts.copilot.ai.web.rest.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for copilot completion, explain, and optimize endpoints.
 */
public record CopilotRequest(
        @NotBlank String prompt,
        String context
) {
}
