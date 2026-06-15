package vn.xime.application.domain.application.exception;

/**
 * Raised when registering an application with an already-used application_code.
 * Ném khi đăng ký application với application_code đã tồn tại.
 *
 * Exception nghiệp vụ thuần - adapter map sang ErrorCode.DUPLICATE_APPLICATION_CODE.
 */
public class DuplicateApplicationCodeException extends RuntimeException {

    public DuplicateApplicationCodeException(String applicationCode) {
        super("Application code already exists: " + applicationCode);
    }
}
