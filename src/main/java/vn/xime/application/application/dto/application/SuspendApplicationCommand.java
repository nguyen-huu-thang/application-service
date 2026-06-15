package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to suspend an application (ACTIVE -> SUSPENDED).
 * Command tạm khóa application (ACTIVE -> SUSPENDED).
 */
public record SuspendApplicationCommand(
        ApplicationId identityId
) {
}
