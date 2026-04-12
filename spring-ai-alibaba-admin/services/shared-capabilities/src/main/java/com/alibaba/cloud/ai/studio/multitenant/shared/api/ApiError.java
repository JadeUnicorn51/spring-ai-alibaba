package com.alibaba.cloud.ai.studio.multitenant.shared.api;

public record ApiError(String code, String message, String traceId) {
}
