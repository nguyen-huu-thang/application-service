package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to retire (soft-delete) a disabled application (DISABLED -> RETIRED).
 * Command xóa mềm application đã vô hiệu hóa (DISABLED -> RETIRED).
 */
public record RetireApplicationCommand(
        ApplicationId identityId
) {
}
