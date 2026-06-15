package vn.xime.application.application.dto.application;

/**
 * Command to register a new application.
 * Command đăng ký application mới.
 *
 * Mang dữ liệu thô (chưa dựng value object) - use case build VO để rule validate
 * nằm trong domain.
 */
public record RegisterApplicationCommand(
        String applicationCode,
        String name,
        String description
) {
}
