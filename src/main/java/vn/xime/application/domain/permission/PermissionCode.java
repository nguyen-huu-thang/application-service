package vn.xime.application.domain.permission;

/**
 * System permissions that the platform can grant to an application.
 * Danh sách quyền hệ thống nền tảng có thể cấp cho một application.
 *
 * Mở rộng khi thêm resource service. Lưu DB dưới dạng tên enum (VARCHAR).
 */
public enum PermissionCode {

    DATA_CREATE_OBJECT,
    DATA_READ_OBJECT,
    DATA_UPDATE_OBJECT,
    DATA_DELETE_OBJECT,
    DATA_CREATE_SCHEMA,
    DATA_READ_SCHEMA;

    /**
     * Parses a permission name, throwing IllegalArgumentException on unknown value.
     * Phân giải tên quyền, ném IllegalArgumentException nếu không hợp lệ.
     */
    public static PermissionCode from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("permission cannot be null");
        }
        try {
            return PermissionCode.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown permission code: " + raw);
        }
    }
}
