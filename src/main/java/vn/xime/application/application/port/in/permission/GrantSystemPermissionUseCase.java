package vn.xime.application.application.port.in.permission;

import vn.xime.application.application.dto.permission.GrantSystemPermissionCommand;

/**
 * Grants a system permission to an application.
 * Cấp một quyền hệ thống cho application.
 */
public interface GrantSystemPermissionUseCase {

    void grant(GrantSystemPermissionCommand command);
}
