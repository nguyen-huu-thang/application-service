package vn.xime.application.application.port.in.permission;

import vn.xime.application.application.dto.permission.RevokeSystemPermissionCommand;

/**
 * Revokes a system permission from an application.
 * Thu hồi một quyền hệ thống khỏi application.
 */
public interface RevokeSystemPermissionUseCase {

    void revoke(RevokeSystemPermissionCommand command);
}
