package vn.xime.application.common.exception;

import vn.xime.application.domain.error.ErrorCode;

/**
 * Base application exception carrying an ErrorCode from the catalog.
 * Exception nền mang một ErrorCode trong catalog.
 *
 * Không dùng trực tiếp; ném qua một trong ba lớp con theo mức phơi bày:
 * PrivateError / SystemError / PublicError.
 */
public abstract class AppException extends RuntimeException {

    private final ErrorCode errorCode;

    protected AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected AppException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
