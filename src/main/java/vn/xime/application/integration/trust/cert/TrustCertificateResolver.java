package vn.xime.application.integration.trust.cert;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import vn.xime.application.integration.trust.model.Certificate;

/**
 * Runtime in-memory cache of the active service certificate.
 * Cache RAM của certificate runtime đang hoạt động.
 *
 * Chỉ giữ và phơi bày certificate hiện tại; không gRPC, không persistence, không scheduler.
 */
@Component
public class TrustCertificateResolver {

    private final AtomicReference<Certificate> certificate = new AtomicReference<>();

    public Optional<Certificate> resolve() {
        return Optional.ofNullable(certificate.get());
    }

    public void update(Certificate newCertificate) {
        certificate.set(newCertificate);
    }

    public void remove() {
        certificate.set(null);
    }

    public void cleanExpiredCertificate() {
        Certificate current = certificate.get();
        if (current == null) {
            return;
        }
        Instant expiresAt = current.expiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            certificate.set(null);
        }
    }

    public boolean hasCertificate() {
        return certificate.get() != null;
    }

    public void clear() {
        certificate.set(null);
    }
}
