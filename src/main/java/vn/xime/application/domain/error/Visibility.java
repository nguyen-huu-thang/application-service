package vn.xime.application.domain.error;

/**
 * Error exposure level (decreasing security): which channel may see the real error.
 * Mức độ phơi bày của lỗi (bảo mật giảm dần): kênh nào được thấy lỗi thật.
 *
 * - PRIVATE: chỉ tồn tại trong service này (log nội bộ).
 * - SYSTEM:  service khác đọc được qua gRPC nội bộ, nhưng không ra browser.
 * - PUBLIC:  browser / REST external đọc được.
 */
public enum Visibility {
    PRIVATE,
    SYSTEM,
    PUBLIC
}
