package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to reactivate a suspended application (SUSPENDED -> ACTIVE).
 * Command mở lại application đang tạm khóa (SUSPENDED -> ACTIVE).
 */
public record ReactivateApplicationCommand(
        ApplicationId identityId
) {
}
