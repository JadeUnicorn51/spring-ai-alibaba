package com.alibaba.cloud.ai.studio.multitenant.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alibaba.cloud.ai.studio.multitenant.shared.api.ApiResponse;
import com.alibaba.cloud.ai.studio.multitenant.shared.context.RequestContext;
import com.alibaba.cloud.ai.studio.multitenant.shared.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.multitenant.shared.exception.ServiceException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiResponse.failure(ex.getCode(), ex.getMessage(), traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.failure("INTERNAL_ERROR", ex.getMessage(), traceId()));
    }

    private String traceId() {
        return RequestContextHolder.get().map(RequestContext::traceId).orElse("n/a");
    }

}
