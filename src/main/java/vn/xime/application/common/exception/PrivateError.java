package vn.xime.application.common.exception;

import vn.xime.application.domain.error.ErrorCode;

/**
 * Error that must stay inside this service (highest security).
 * Lỗi chỉ được tồn tại trong chính service này (bảo mật cao nhất).
 *
 * Adapter che thành mã generic ở mọi kênh ra ngoài (gRPC lẫn REST).
 */
public class PrivateError extends AppException {

    public PrivateError(ErrorCode errorCode) {
        super(errorCode);
    }

    public PrivateError(ErrorCode errorCode, String overrideMessage) {
        super(errorCode, overrideMessage);
    }
}
