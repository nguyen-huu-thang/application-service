package vn.xime.application.application.dto.permission;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to revoke a system permission from an application.
 * Command thu hồi một quyền hệ thống khỏi application.
 *
 * permission là tên thô (vd "DATA_READ_OBJECT") - use case phân giải sang PermissionCode.
 */
public record RevokeSystemPermissionCommand(
        ApplicationId appId,
        String permission
) {
}
