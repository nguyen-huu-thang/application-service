package vn.xime.application.application.dto.permission;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Command to grant a system permission to an application.
 * Command cấp một quyền hệ thống cho application.
 *
 * permission là tên thô (vd "DATA_READ_OBJECT") - use case phân giải sang PermissionCode.
 */
public record GrantSystemPermissionCommand(
        ApplicationId appId,
        String permission
) {
}
