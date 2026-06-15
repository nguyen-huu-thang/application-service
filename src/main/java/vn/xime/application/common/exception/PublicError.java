package vn.xime.application.common.exception;

import vn.xime.application.domain.error.ErrorCode;

/**
 * Error safe for browsers / external REST clients (lowest security).
 * Lỗi an toàn cho browser / REST external (bảo mật thấp nhất).
 *
 * Adapter giữ nguyên ở mọi kênh ra ngoài.
 */
public class PublicError extends AppException {

    public PublicError(ErrorCode errorCode) {
        super(errorCode);
    }

    public PublicError(ErrorCode errorCode, String overrideMessage) {
        super(errorCode, overrideMessage);
    }
}
