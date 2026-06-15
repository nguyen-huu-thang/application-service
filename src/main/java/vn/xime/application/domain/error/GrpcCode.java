package vn.xime.application.domain.error;

/**
 * Language-neutral gRPC status code, mirroring the names of io.grpc.Status.Code.
 * Mã trạng thái gRPC trung lập, trùng tên với io.grpc.Status.Code.
 *
 * Giữ ở tầng domain để domain không phụ thuộc io.grpc. Adapter gRPC chuyển
 * sang io.grpc.Status.Code bằng valueOf(name()).
 */
public enum GrpcCode {
    INVALID_ARGUMENT,
    NOT_FOUND,
    ALREADY_EXISTS,
    PERMISSION_DENIED,
    UNAUTHENTICATED,
    FAILED_PRECONDITION,
    UNAVAILABLE,
    DEADLINE_EXCEEDED,
    RESOURCE_EXHAUSTED,
    INTERNAL
}
