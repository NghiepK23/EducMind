package com.educmind.common.envelope;

/**
 * Chi tiết lỗi trong ApiResponse.error — {code, message}.
 */
public class ErrorDetail {

    private final String code;
    private final String message;

    public ErrorDetail(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
