package vn.xime.application.domain.error;

/**
 * Redacts an error code according to the outgoing channel and its visibility.
 * Che mã lỗi theo kênh ra và mức phơi bày của nó.
 *
 * Quy tắc:
 * - PUBLIC: ra mọi kênh.
 * - SYSTEM: ra gRPC nội bộ; ở REST external bị che về mã common cùng họ.
 * - PRIVATE: bị che về UNKNOWN ở mọi kênh.
 */
public final class ErrorRedactor {

    private ErrorRedactor() {
    }

    public static ErrorCode forChannel(ErrorCode errorCode, Channel channel) {
        Visibility v = errorCode.getVisibility();

        boolean allowed = switch (channel) {
            case GRPC_INTERNAL -> v == Visibility.SYSTEM || v == Visibility.PUBLIC;
            case REST_EXTERNAL -> v == Visibility.PUBLIC;
        };

        if (allowed) {
            return errorCode;
        }
        if (v == Visibility.PRIVATE) {
            return ErrorCode.UNKNOWN;
        }
        // SYSTEM bị che ở REST: giữ ngữ nghĩa họ trạng thái, giấu chi tiết
        return ErrorCode.genericFor(errorCode);
    }
}
