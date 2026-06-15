package vn.xime.application.domain.application.event;

import java.time.Instant;

import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Raised when an application is permanently disabled.
 * Raise khi application bị vô hiệu hóa vĩnh viễn.
 */
public record ApplicationDisabledEvent(
        ApplicationId applicationId,
        Instant occurredAt
) implements DomainEvent {
}
