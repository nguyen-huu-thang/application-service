package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to activate an application (PENDING_REVIEW -> ACTIVE).
 * Command kích hoạt application (PENDING_REVIEW -> ACTIVE).
 */
public record ActivateApplicationCommand(
        ApplicationId identityId
) {
}
