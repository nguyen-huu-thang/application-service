package vn.xime.application.domain.application.event;

import java.time.Instant;

import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Raised when a new application is registered (status PENDING_REVIEW).
 * Raise khi đăng ký application mới (trạng thái PENDING_REVIEW).
 */
public record ApplicationRegisteredEvent(
        ApplicationId applicationId,
        String applicationCode,
        String name,
        Instant occurredAt
) implements DomainEvent {
}
