package vn.xime.application.common.exception;

import vn.xime.application.domain.error.ErrorCode;

/**
 * Error readable by internal services over gRPC mTLS, but hidden from browsers.
 * Lỗi service khác đọc được qua gRPC nội bộ, nhưng bị che với browser.
 *
 * Adapter giữ nguyên ở kênh gRPC nội bộ; che thành mã common cùng họ ở REST external.
 */
public class SystemError extends AppException {

    public SystemError(ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemError(ErrorCode errorCode, String overrideMessage) {
        super(errorCode, overrideMessage);
    }
}
