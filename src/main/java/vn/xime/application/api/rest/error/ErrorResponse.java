package vn.xime.application.api.rest.error;

/**
 * Standard REST error body for the whole platform: {errorKey, code, message}.
 * Body lỗi REST chuẩn toàn platform: {errorKey, code, message}.
 */
public record ErrorResponse(
        String errorKey,
        int code,
        String message
) {
}
