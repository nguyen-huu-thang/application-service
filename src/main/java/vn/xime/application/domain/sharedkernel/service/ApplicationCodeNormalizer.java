package vn.xime.application.domain.sharedkernel.service;

import java.util.regex.Pattern;

/**
 * Normalizes and validates application_code (lowercase slug, unique per platform).
 * Chuẩn hóa và kiểm tra application_code (slug chữ thường, unique toàn platform).
 *
 * Quy tắc: lowercase + trim trước mọi thao tác lưu/tìm. Cho phép chữ thường,
 * chữ số và dấu "-" ở giữa (vd "xime-social"); không bắt đầu/kết thúc bằng "-".
 * Độ dài 2-64. Trả về chuỗi đã chuẩn hóa hoặc ném IllegalArgumentException.
 */
public final class ApplicationCodeNormalizer {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 64;

    // lowercase alnum, optional internal hyphens; no leading/trailing hyphen
    private static final Pattern PATTERN =
            Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");

    private ApplicationCodeNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("application_code cannot be null");
        }

        String normalized = raw.trim().toLowerCase();

        if (normalized.length() < MIN_LENGTH || normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "application_code must be " + MIN_LENGTH + "-" + MAX_LENGTH + " characters"
            );
        }

        if (!PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "application_code must be lowercase alphanumeric with internal hyphens only"
            );
        }

        return normalized;
    }
}
