package vn.xime.application.domain.error;

/**
 * Centralized error catalog for application-service.
 * Catalog mã lỗi tập trung của application-service.
 *
 * Theo chuẩn platform (xem giới thiệu/.claude/docs/cross-cutting/quy-uoc-ma-loi-va-exception.md):
 * - common 000000-009999 (dùng chung + làm mã generic khi che lỗi)
 * - application-service block 030000-039999
 *   (Private 030000-033999 / System 034000-036999 / Public 037000-039999)
 *
 * Domain pure: dùng int httpStatus + GrpcCode (không phụ thuộc Spring/io.grpc).
 */
public enum ErrorCode {

    // ===== common - Private (lỗi hạ tầng, không ra ngoài) =====
    UNKNOWN("E000000", 0, 500, GrpcCode.INTERNAL, Visibility.PRIVATE, "Lỗi không xác định"),
    INTERNAL_ERROR("E000001", 1, 500, GrpcCode.INTERNAL, Visibility.PRIVATE, "Lỗi nội bộ hệ thống"),
    DATABASE_ERROR("E000002", 2, 500, GrpcCode.INTERNAL, Visibility.PRIVATE, "Lỗi cơ sở dữ liệu"),
    CONFIG_ERROR("E000003", 3, 500, GrpcCode.INTERNAL, Visibility.PRIVATE, "Lỗi cấu hình"),

    // ===== common - System (lỗi liên service, chỉ service nội bộ đọc) =====
    UPSTREAM_ERROR("E004000", 4000, 502, GrpcCode.INTERNAL, Visibility.SYSTEM, "Lỗi gọi service nội bộ"),
    DEPENDENCY_UNAVAILABLE("E004001", 4001, 503, GrpcCode.UNAVAILABLE, Visibility.SYSTEM, "Service phụ thuộc không khả dụng"),
    UPSTREAM_TIMEOUT("E004002", 4002, 504, GrpcCode.DEADLINE_EXCEEDED, Visibility.SYSTEM, "Hết thời gian chờ service nội bộ"),
    INTER_SERVICE_AUTH_FAILED("E004003", 4003, 401, GrpcCode.UNAUTHENTICATED, Visibility.SYSTEM, "Xác thực liên service thất bại"),

    // ===== common - Public (an toàn cho client, dùng làm generic khi che) =====
    BAD_REQUEST("E007000", 7000, 400, GrpcCode.INVALID_ARGUMENT, Visibility.PUBLIC, "Yêu cầu không hợp lệ"),
    VALIDATION_FAILED("E007001", 7001, 400, GrpcCode.INVALID_ARGUMENT, Visibility.PUBLIC, "Dữ liệu đầu vào không hợp lệ"),
    UNAUTHENTICATED("E007002", 7002, 401, GrpcCode.UNAUTHENTICATED, Visibility.PUBLIC, "Chưa xác thực"),
    SESSION_EXPIRED("E007003", 7003, 401, GrpcCode.UNAUTHENTICATED, Visibility.PUBLIC, "Phiên làm việc đã hết hạn"),
    FORBIDDEN("E007004", 7004, 403, GrpcCode.PERMISSION_DENIED, Visibility.PUBLIC, "Không có quyền truy cập"),
    NOT_FOUND("E007005", 7005, 404, GrpcCode.NOT_FOUND, Visibility.PUBLIC, "Không tìm thấy tài nguyên"),
    ALREADY_EXISTS("E007006", 7006, 409, GrpcCode.ALREADY_EXISTS, Visibility.PUBLIC, "Tài nguyên đã tồn tại"),
    RULE_VIOLATION("E007007", 7007, 422, GrpcCode.FAILED_PRECONDITION, Visibility.PUBLIC, "Vi phạm ràng buộc nghiệp vụ"),
    TOO_MANY_REQUESTS("E007008", 7008, 429, GrpcCode.RESOURCE_EXHAUSTED, Visibility.PUBLIC, "Quá nhiều yêu cầu"),

    // ===== application-service - Private (030000-033999): hạ tầng nội bộ, không ra ngoài =====
    OUTBOX_PUBLISH_FAILED("E030000", 30000, 500, GrpcCode.INTERNAL, Visibility.PRIVATE, "Lỗi phát sự kiện outbox"),
    EVENT_SERIALIZATION_FAILED("E030001", 30001, 500, GrpcCode.INTERNAL, Visibility.PRIVATE, "Lỗi tuần tự hóa sự kiện"),

    // ===== application-service - System (034000-036999): service khác đọc qua gRPC nội bộ =====
    SUBJECT_NOT_FOUND("E034000", 34000, 404, GrpcCode.NOT_FOUND, Visibility.SYSTEM, "Không tìm thấy subject"),

    // ===== application-service - Public (037000-039999): an toàn cho client =====
    APPLICATION_NOT_FOUND("E037000", 37000, 404, GrpcCode.NOT_FOUND, Visibility.PUBLIC, "Không tìm thấy application"),
    DUPLICATE_APPLICATION_CODE("E037001", 37001, 409, GrpcCode.ALREADY_EXISTS, Visibility.PUBLIC, "application_code đã tồn tại"),
    INVALID_STATUS_TRANSITION("E037002", 37002, 422, GrpcCode.FAILED_PRECONDITION, Visibility.PUBLIC, "Chuyển trạng thái không hợp lệ"),
    INVALID_APPLICATION_CODE("E037003", 37003, 400, GrpcCode.INVALID_ARGUMENT, Visibility.PUBLIC, "application_code không hợp lệ"),
    INVALID_APPLICATION_NAME("E037004", 37004, 400, GrpcCode.INVALID_ARGUMENT, Visibility.PUBLIC, "Tên application không hợp lệ"),
    INVALID_APPLICATION_DESCRIPTION("E037005", 37005, 400, GrpcCode.INVALID_ARGUMENT, Visibility.PUBLIC, "Mô tả application không hợp lệ"),
    PERMISSION_ALREADY_GRANTED("E037010", 37010, 409, GrpcCode.ALREADY_EXISTS, Visibility.PUBLIC, "Quyền đã được cấp"),
    PERMISSION_NOT_GRANTED("E037011", 37011, 422, GrpcCode.FAILED_PRECONDITION, Visibility.PUBLIC, "Quyền chưa được cấp"),
    INVALID_PERMISSION_CODE("E037012", 37012, 400, GrpcCode.INVALID_ARGUMENT, Visibility.PUBLIC, "Mã quyền không hợp lệ");

    private final String errorKey;
    private final int code;
    private final int httpStatus;
    private final GrpcCode grpcCode;
    private final Visibility visibility;
    private final String message;

    ErrorCode(String errorKey, int code, int httpStatus,
              GrpcCode grpcCode, Visibility visibility, String message) {
        this.errorKey = errorKey;
        this.code = code;
        this.httpStatus = httpStatus;
        this.grpcCode = grpcCode;
        this.visibility = visibility;
        this.message = message;
    }

    public String getErrorKey() {
        return errorKey;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public GrpcCode getGrpcCode() {
        return grpcCode;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Generic common code in the same status family, used when redacting a hidden error.
     * Mã common cùng họ trạng thái, dùng khi che một lỗi không được phép phơi bày.
     */
    public static ErrorCode genericFor(ErrorCode ec) {
        return switch (ec.grpcCode) {
            case INVALID_ARGUMENT -> BAD_REQUEST;
            case NOT_FOUND -> NOT_FOUND;
            case ALREADY_EXISTS -> ALREADY_EXISTS;
            case PERMISSION_DENIED -> FORBIDDEN;
            case UNAUTHENTICATED -> UNAUTHENTICATED;
            case FAILED_PRECONDITION -> RULE_VIOLATION;
            case RESOURCE_EXHAUSTED -> TOO_MANY_REQUESTS;
            default -> UNKNOWN;
        };
    }
}
