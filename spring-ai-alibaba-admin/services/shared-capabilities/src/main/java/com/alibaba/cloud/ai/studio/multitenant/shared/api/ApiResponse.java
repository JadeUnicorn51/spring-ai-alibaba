package com.alibaba.cloud.ai.studio.multitenant.shared.api;

import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String code, String message, String traceId) {
        return new ApiResponse<>(false, null, new ApiError(code, message, traceId), Instant.now());
    }

}
