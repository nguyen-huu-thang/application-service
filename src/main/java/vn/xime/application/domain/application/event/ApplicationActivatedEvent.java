package vn.xime.application.domain.application.event;

import java.time.Instant;

import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Raised when an application transitions to ACTIVE.
 * Raise khi application chuyển sang ACTIVE.
 */
public record ApplicationActivatedEvent(
        ApplicationId applicationId,
        Instant occurredAt
) implements DomainEvent {
}
