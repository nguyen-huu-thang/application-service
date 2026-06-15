package vn.xime.application.domain.permission.event;

import java.time.Instant;

import vn.xime.application.domain.permission.PermissionCode;
import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Raised when a system permission is granted to an application.
 * Raise khi một quyền hệ thống được cấp cho application.
 */
public record SystemPermissionGrantedEvent(
        ApplicationId applicationId,
        PermissionCode permission,
        Instant occurredAt
) implements DomainEvent {
}
