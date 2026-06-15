package vn.xime.application.domain.application.event;

import java.time.Instant;

import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Raised when an application is soft-deleted (RETIRED).
 * Raise khi application bị xóa mềm (RETIRED).
 */
public record ApplicationRetiredEvent(
        ApplicationId applicationId,
        Instant occurredAt
) implements DomainEvent {
}
