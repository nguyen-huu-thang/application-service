package vn.xime.application.domain.error;

/**
 * Outgoing channel an error is being sent through (decides redaction).
 * Kênh mà lỗi đang được gửi ra (quyết định việc che lỗi).
 */
public enum Channel {
    /** REST external - browser; chỉ phơi bày PUBLIC. */
    REST_EXTERNAL,
    /** gRPC nội bộ giữa các service; phơi bày SYSTEM + PUBLIC. */
    GRPC_INTERNAL
}
