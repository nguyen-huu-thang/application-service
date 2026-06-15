package vn.xime.application.api.rest.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import vn.xime.application.api.error.ExceptionErrorCodeResolver;
import vn.xime.application.domain.error.Channel;
import vn.xime.application.domain.error.ErrorCode;
import vn.xime.application.domain.error.ErrorRedactor;
import vn.xime.application.domain.error.Visibility;

/**
 * Global REST exception handler - maps exceptions to the standard error body.
 * Bộ xử lý exception REST toàn cục - map exception sang body lỗi chuẩn.
 *
 * Dùng chung ExceptionErrorCodeResolver với gRPC; che lỗi theo kênh REST_EXTERNAL nên
 * lỗi PRIVATE/SYSTEM không lọt ra browser. REST của service này hiện chỉ phục vụ actuator,
 * nhưng handler vẫn đặt sẵn cho mọi REST endpoint thêm sau và để đồng bộ chuẩn platform.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean validation (@Valid) failure -> 400.
     * Lỗi validate body theo @Valid -> 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        String message = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getField() + ": "
                  + ex.getBindingResult().getFieldError().getDefaultMessage()
                : ec.getMessage();
        return toResponse(ec, message);
    }

    /**
     * Business / domain / known runtime exceptions resolved via the shared resolver.
     * Lỗi nghiệp vụ / domain / runtime đã biết, phân giải qua resolver dùng chung.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        ErrorCode original = ExceptionErrorCodeResolver.resolve(ex);

        if (original.getVisibility() != Visibility.PUBLIC) {
            log.error("Non-public error reached REST: {}", original.getErrorKey(), ex);
        }

        ErrorCode ec = ErrorRedactor.forChannel(original, Channel.REST_EXTERNAL);
        String message = original.getVisibility() == Visibility.PUBLIC
                ? coalesce(ex.getMessage(), original.getMessage())
                : ec.getMessage();
        return toResponse(ec, message);
    }

    /**
     * Fallback - never leak details.
     * Dự phòng - không bao giờ lộ chi tiết.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorCode ec = ErrorCode.UNKNOWN;
        return toResponse(ec, ec.getMessage());
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode ec, String message) {
        return ResponseEntity
                .status(ec.getHttpStatus())
                .body(new ErrorResponse(ec.getErrorKey(), ec.getCode(), message));
    }

    private static String coalesce(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
