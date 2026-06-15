package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to permanently disable an application (ACTIVE|SUSPENDED -> DISABLED).
 * Command vô hiệu hóa vĩnh viễn application (ACTIVE|SUSPENDED -> DISABLED).
 */
public record DisableApplicationCommand(
        ApplicationId identityId
) {
}
