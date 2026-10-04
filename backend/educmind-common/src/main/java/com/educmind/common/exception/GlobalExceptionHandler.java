package com.educmind.common.exception;

import com.educmind.common.envelope.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Bắt exception toàn hệ thống, convert thành ApiResponse.error đúng format chuẩn.
 * Đặt ở educmind-common vì áp dụng cho mọi Controller ở mọi module.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    /**
     * Route không tồn tại (vd: gọi sai path, hoặc endpoint chưa được cài đặt) — Spring MVC
     * mặc định coi đây là "tìm static resource" nên ném NoResourceFoundException thay vì
     * lỗi 404 thông thường. Bắt riêng ở đây để trả đúng 404, tránh bị handleUnexpectedException
     * (bên dưới) nuốt chung thành 500 gây khó debug (đã từng xảy ra thực tế: endpoint
     * GET /api/v1/users/me chưa viết Controller, log ra 500 trong khi bản chất là 404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("Route not found: {}", ex.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("NOT_FOUND", "Đường dẫn không tồn tại"));
    }

    /**
     * User đã xác thực (JWT hợp lệ) nhưng không đủ role — Spring Security 6.3+ ném
     * AuthorizationDeniedException từ @PreAuthorize thay vì AccessDeniedException cũ.
     * Bắt riêng ở đây để trả đúng 403, tránh bị handleUnexpectedException nuốt chung
     * thành 500 (đã xảy ra thực tế khi thêm @PreAuthorize("hasRole('SCHOOL_ADMIN')")
     * vào POST /api/v1/users — STUDENT gọi vào bị 500 thay vì 403 đúng nghĩa).
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Bạn không có quyền thực hiện thao tác này"));
    }

    /**
     * Gọi đúng path nhưng sai HTTP method (vd: GET vào endpoint chỉ nhận POST) — Spring MVC
     * ném HttpRequestMethodNotSupportedException, mặc định nên trả 405 Method Not Allowed.
     * Bắt riêng ở đây để trả đúng 405, tránh bị handleUnexpectedException nuốt chung thành 500
     * (phát hiện thực tế khi gọi GET /api/v1/auth/login — vốn chỉ nhận POST — bị 500 thay vì 405).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {} {}", ex.getMethod(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("METHOD_NOT_ALLOWED", "Phương thức HTTP không được hỗ trợ cho đường dẫn này"));
    }

    /**
     * Tham số (query/path) sai kiểu, vd: class_id=abc không ép được sang UUID — Spring MVC ném
     * MethodArgumentTypeMismatchException. Bắt riêng để trả 400 thay vì bị handleUnexpectedException
     * nuốt thành 500 (phát hiện khi test GET .../mastery-matrix?class_id=abc).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid parameter type: {}", ex.getName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_FILTER",
                        "Tham số " + ex.getName() + " không hợp lệ"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected exception", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "Đã có lỗi xảy ra, vui lòng thử lại sau"));
    }
}
