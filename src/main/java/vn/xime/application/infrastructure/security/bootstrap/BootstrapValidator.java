package vn.xime.application.infrastructure.security.bootstrap;

import java.time.Instant;
import java.util.Objects;

/**
 * Validates structural + identity + lifecycle integrity of a bootstrap payload.
 * Kiểm tra tính toàn vẹn cấu trúc + định danh + vòng đời của bootstrap payload.
 *
 * Sai bất kỳ điều kiện nào = fatal, dừng khởi động ngay (không thiết lập được trust).
 */
public class BootstrapValidator {

    public void validate(String currentServiceId, BootstrapPayload payload) {

        requireNotBlank(currentServiceId,
                "Current service ID is missing", "Thiếu service ID hiện tại");

        if (payload == null) {
            fatal("Bootstrap payload is null", "Bootstrap payload bị null");
        }

        BootstrapPayload.Certificate certificate = payload.certificate();
        if (certificate == null) {
            fatal("Bootstrap certificate is missing", "Thiếu bootstrap certificate");
        }

        requireNotBlank(certificate.id(),
                "Bootstrap certificate ID is missing", "Thiếu bootstrap certificate ID");
        requireNotBlank(certificate.serviceId(),
                "Bootstrap certificate service ID is missing", "Thiếu service ID trong bootstrap certificate");
        requireNotBlank(certificate.publicCert(),
                "Bootstrap public certificate is missing", "Thiếu public certificate");
        requireNotBlank(certificate.privateKey(),
                "Bootstrap private key is missing", "Thiếu private key");
        requireNotBlank(payload.tokenId(),
                "Bootstrap token ID is missing", "Thiếu bootstrap token ID");
        requireNotBlank(payload.refreshToken(),
                "Bootstrap refresh token is missing", "Thiếu bootstrap refresh token");

        if (!Objects.equals(currentServiceId, certificate.serviceId())) {
            fatal(
                    "Bootstrap certificate service ID mismatch. Expected: %s, Actual: %s"
                            .formatted(currentServiceId, certificate.serviceId()),
                    "Service ID của bootstrap certificate không khớp. Mong đợi: %s, Thực tế: %s"
                            .formatted(currentServiceId, certificate.serviceId()));
        }

        if (certificate.deleted()) {
            fatal("Bootstrap certificate has been deleted", "Bootstrap certificate đã bị xóa");
        }

        requireNotBlank(certificate.status(),
                "Bootstrap certificate status is missing", "Thiếu trạng thái bootstrap certificate");

        if (!"ACTIVE".equals(certificate.status())) {
            fatal(
                    "Bootstrap certificate is not ACTIVE. Actual status: %s".formatted(certificate.status()),
                    "Bootstrap certificate không ở trạng thái ACTIVE. Trạng thái hiện tại: %s"
                            .formatted(certificate.status()));
        }

        Instant now = Instant.now();

        Instant expiresAt = certificate.expiresAtInstant();
        if (expiresAt.isBefore(now)) {
            fatal(
                    "Bootstrap certificate has expired. Expires at: %s".formatted(expiresAt),
                    "Bootstrap certificate đã hết hạn. Hết hạn lúc: %s".formatted(expiresAt));
        }

        Instant issuedAt = certificate.issuedAtInstant();
        if (issuedAt.isAfter(now.plusSeconds(60))) {
            fatal(
                    "Bootstrap certificate issued_at is invalid. issued_at: %s".formatted(issuedAt),
                    "issued_at của bootstrap certificate không hợp lệ. issued_at: %s".formatted(issuedAt));
        }
    }

    private void requireNotBlank(String value, String english, String vietnamese) {
        if (value == null || value.isBlank()) {
            fatal(english, vietnamese);
        }
    }

    private void fatal(String english, String vietnamese) {
        String message = """
                ==================================================
                FATAL BOOTSTRAP SECURITY ERROR
                ==================================================
                [ ENGLISH ] %s
                [ TIẾNG VIỆT ] %s
                System startup terminated immediately.
                ==================================================
                """.formatted(english, vietnamese);

        System.err.println(message);
        throw new IllegalStateException(message);
    }
}
