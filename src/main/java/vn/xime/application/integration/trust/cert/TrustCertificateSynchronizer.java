package vn.xime.application.integration.trust.cert;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import vn.xime.application.infrastructure.grpc.trust.cert.GrpcTrustCertificateClient;
import vn.xime.application.infrastructure.persistence.repository.CertificateRepository;
import vn.xime.application.infrastructure.security.bootstrap.Bootstrap;
import vn.xime.application.integration.trust.model.Certificate;
import vn.xime.application.integration.trust.ssl.TrustSslContextProvider;

/**
 * Establishes and maintains this service's runtime certificate against Trust.
 * Thiết lập và duy trì certificate runtime của service với Trust.
 *
 * Khác user-service: TrustSslContextProvider được inject final (user-service để field
 * không inject -> null, chỉ không NPE vì scheduler 24h chưa bật). Ở đây scheduling bật nên
 * phải inject đúng để synchronize() reload SSL được.
 *
 * Trạng thái startup: NEW (có bootstrap, chưa có DB cert), ACTIVE (chỉ DB cert),
 * RECOVERABLE (có cả hai), BROKEN (không có gì -> fatal).
 */
@Component
@RequiredArgsConstructor
public class TrustCertificateSynchronizer {

    private final GrpcTrustCertificateClient grpcTrustCertificateClient;
    private final TrustCertificateResolver trustCertificateResolver;
    private final CertificateRepository certificateRepository;
    private final Bootstrap bootstrap;
    private final TrustSslContextProvider ssl;

    // =========================
    // STARTUP SYNCHRONIZATION
    // =========================

    public void synchronizeOnStartup() {
        boolean hasBootstrap = bootstrap.exists();

        Optional<Certificate> databaseCertificate = certificateRepository.findLatest();
        boolean hasDatabaseCertificate = databaseCertificate.isPresent();

        // BROKEN
        if (!hasBootstrap && !hasDatabaseCertificate) {
            throw new IllegalStateException("""

                    ==================================================
                    FATAL TRUST STARTUP ERROR
                    ==================================================
                    No bootstrap file found. No runtime certificate found in database.
                    System cannot establish trust.

                    Bootstrap file không tồn tại. Database không có runtime certificate.
                    Hệ thống không thể thiết lập trust.
                    ==================================================
                    """);
        }

        // NEW
        if (hasBootstrap && !hasDatabaseCertificate) {
            synchronizeBootstrap();
            return;
        }

        // ACTIVE
        if (!hasBootstrap && hasDatabaseCertificate) {
            synchronizeRuntime(databaseCertificate.get());
            return;
        }

        // RECOVERABLE
        synchronizeRecoverable();
    }

    // =========================
    // SCHEDULED SYNCHRONIZATION
    // =========================

    public void synchronize() {
        Optional<Certificate> certificate = certificateRepository.findLatest();
        if (certificate.isEmpty()) {
            return;
        }

        synchronizeRuntime(certificate.get());
        ssl.reload();
        certificateRepository.deleteExpiredCertificates(Instant.now());
    }

    // =========================
    // FLOWS
    // =========================

    private void synchronizeBootstrap() {
        Certificate bootstrapCertificate = bootstrap.load();
        publish(bootstrapCertificate);

        Certificate rotatedCertificate = rotateCertificate(bootstrapCertificate);
        publish(rotatedCertificate);

        bootstrap.delete();
    }

    private void synchronizeRuntime(Certificate currentCertificate) {
        // cập nhật cache runtime ngay
        trustCertificateResolver.update(currentCertificate);

        // rotate sau 5 tháng (150 ngày)
        Instant now = Instant.now();
        Instant issuedAt = currentCertificate.issuedAt();

        if (issuedAt != null && issuedAt.plus(150, ChronoUnit.DAYS).isAfter(now)) {
            return;
        }

        try {
            Certificate rotatedCertificate = rotateCertificate(currentCertificate);
            publish(rotatedCertificate);
        } catch (RuntimeException exception) {
            // giữ cert hiện tại còn hợp lệ, không làm gãy runtime
        }
    }

    private void synchronizeRecoverable() {
        certificateRepository.deleteAll();
        synchronizeBootstrap();
    }

    private Certificate rotateCertificate(Certificate certificate) {
        return grpcTrustCertificateClient.rotateCertificate(
                certificate.refreshTokenId(),
                certificate.refreshToken(),
                certificate.privateKey());
    }

    private void publish(Certificate certificate) {
        certificateRepository.save(certificate);
        trustCertificateResolver.update(certificate);
    }
}
