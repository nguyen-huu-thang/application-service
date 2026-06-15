package vn.xime.application.domain.application.exception;

/**
 * Raised when an application cannot be found by id or code.
 * Ném khi không tìm thấy application theo id hoặc code.
 *
 * Exception nghiệp vụ thuần - adapter map sang ErrorCode.APPLICATION_NOT_FOUND
 * (hoặc SUBJECT_NOT_FOUND ở kênh internal subject lookup).
 */
public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(String identifier) {
        super("Application not found: " + identifier);
    }
}
