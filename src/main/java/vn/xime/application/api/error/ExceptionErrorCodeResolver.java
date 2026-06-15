package vn.xime.application.api.error;

import vn.xime.application.common.exception.AppException;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;
import vn.xime.application.domain.application.exception.DuplicateApplicationCodeException;
import vn.xime.application.domain.application.exception.InvalidStatusTransitionException;
import vn.xime.application.domain.error.ErrorCode;
import vn.xime.application.domain.permission.exception.PermissionAlreadyGrantedException;
import vn.xime.application.domain.permission.exception.PermissionNotGrantedException;

/**
 * Resolves any throwable to a catalog ErrorCode (shared by gRPC and REST adapters).
 * Phân giải mọi throwable về một ErrorCode trong catalog (dùng chung gRPC + REST).
 *
 * Đây là nơi DUY NHẤT map exception nghiệp vụ THUẦN của domain sang ErrorCode - giữ
 * domain framework-neutral, tránh trùng logic ở hai adapter. Redaction theo kênh do
 * ErrorRedactor đảm nhiệm sau bước này.
 */
public final class ExceptionErrorCodeResolver {

    private ExceptionErrorCodeResolver() {
    }

    public static ErrorCode resolve(Throwable ex) {
        // Exception đã mang sẵn ErrorCode.
        if (ex instanceof AppException appException) {
            return appException.getErrorCode();
        }

        // Exception nghiệp vụ thuần của domain.
        if (ex instanceof ApplicationNotFoundException) {
            return ErrorCode.APPLICATION_NOT_FOUND;
        }
        if (ex instanceof DuplicateApplicationCodeException) {
            return ErrorCode.DUPLICATE_APPLICATION_CODE;
        }
        if (ex instanceof InvalidStatusTransitionException) {
            return ErrorCode.INVALID_STATUS_TRANSITION;
        }
        if (ex instanceof PermissionAlreadyGrantedException) {
            return ErrorCode.PERMISSION_ALREADY_GRANTED;
        }
        if (ex instanceof PermissionNotGrantedException) {
            return ErrorCode.PERMISSION_NOT_GRANTED;
        }

        // Lỗi validate thô từ value object / parse.
        if (ex instanceof IllegalArgumentException) {
            return ErrorCode.VALIDATION_FAILED;
        }
        if (ex instanceof IllegalStateException) {
            return ErrorCode.RULE_VIOLATION;
        }

        return ErrorCode.UNKNOWN;
    }
}
