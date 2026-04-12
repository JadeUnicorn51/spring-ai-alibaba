package com.alibaba.cloud.ai.studio.multitenant.shared.exception;

import org.springframework.http.HttpStatus;

public class ServiceException extends RuntimeException {

    private final String code;

    private final HttpStatus status;

    public ServiceException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public ServiceException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
