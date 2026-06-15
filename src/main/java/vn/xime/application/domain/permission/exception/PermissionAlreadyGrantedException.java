package vn.xime.application.domain.permission.exception;

import vn.xime.application.domain.permission.PermissionCode;

/**
 * Raised when granting a permission that is already granted to the application.
 * Ném khi cấp một quyền đã được cấp cho application.
 *
 * Exception nghiệp vụ thuần - adapter map sang ErrorCode.PERMISSION_ALREADY_GRANTED.
 */
public class PermissionAlreadyGrantedException extends RuntimeException {

    public PermissionAlreadyGrantedException(PermissionCode permission) {
        super("Permission already granted: " + permission);
    }
}
