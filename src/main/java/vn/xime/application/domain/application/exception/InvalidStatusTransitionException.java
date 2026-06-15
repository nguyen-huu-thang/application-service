package vn.xime.application.domain.application.exception;

import vn.xime.application.domain.application.ApplicationStatus;

/**
 * Raised when a lifecycle transition is not allowed by the state machine.
 * Ném khi chuyển trạng thái không được phép trong state machine.
 *
 * Exception nghiệp vụ thuần (không mang ErrorCode) - giữ domain framework-neutral.
 * Adapter map sang ErrorCode.INVALID_STATUS_TRANSITION.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final ApplicationStatus from;
    private final ApplicationStatus to;

    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super("Invalid status transition: " + from + " -> " + to);
        this.from = from;
        this.to = to;
    }

    public ApplicationStatus getFrom() {
        return from;
    }

    public ApplicationStatus getTo() {
        return to;
    }
}
