package vn.xime.application.domain.application;

/**
 * Lifecycle states of an Application subject.
 * Các trạng thái vòng đời của subject Application.
 *
 * Luồng chuyển hợp lệ (guard nằm trong aggregate Application):
 *   PENDING_REVIEW -> ACTIVE -> SUSPENDED -> ACTIVE
 *   ACTIVE|SUSPENDED -> DISABLED -> RETIRED
 */
public enum ApplicationStatus {

    /** Mới đăng ký, chờ admin duyệt. Chưa có identity nào dùng được. */
    PENDING_REVIEW,

    /** Đang hoạt động bình thường. App có thể dùng cert để thao tác dữ liệu. */
    ACTIVE,

    /** Tạm khóa. App vẫn tồn tại nhưng resource service từ chối request. */
    SUSPENDED,

    /** Vô hiệu hóa vĩnh viễn. Không thể reactivate. */
    DISABLED,

    /** Xóa mềm. Chỉ dùng cho lưu trữ/audit, không thể phục hồi. */
    RETIRED
}
