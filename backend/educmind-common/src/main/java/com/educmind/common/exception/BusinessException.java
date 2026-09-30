package com.educmind.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception nghiệp vụ dùng chung toàn hệ thống — mang theo errorCode + HTTP status
 * để GlobalExceptionHandler convert đúng thành ApiResponse.error.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(String errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
