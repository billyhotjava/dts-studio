package com.yuzhi.dts.copilot.ai.web.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false, null, error);
    }
}
