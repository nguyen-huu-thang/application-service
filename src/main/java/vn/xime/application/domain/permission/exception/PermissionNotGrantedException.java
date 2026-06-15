package vn.xime.application.domain.permission.exception;

import vn.xime.application.domain.permission.PermissionCode;

/**
 * Raised when revoking a permission that the application does not have.
 * Ném khi thu hồi một quyền mà application chưa được cấp.
 *
 * Exception nghiệp vụ thuần - adapter map sang ErrorCode.PERMISSION_NOT_GRANTED.
 */
public class PermissionNotGrantedException extends RuntimeException {

    public PermissionNotGrantedException(PermissionCode permission) {
        super("Permission not granted: " + permission);
    }
}
