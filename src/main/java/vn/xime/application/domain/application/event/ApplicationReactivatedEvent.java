package vn.xime.application.domain.application.event;

import java.time.Instant;

import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Raised when an application transitions back to ACTIVE from SUSPENDED.
 * Raise khi application từ SUSPENDED quay lại ACTIVE.
 */
public record ApplicationReactivatedEvent(
        ApplicationId applicationId,
        Instant occurredAt
) implements DomainEvent {
}
