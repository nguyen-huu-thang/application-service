package vn.xime.application.integration.trust.publicca;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

/**
 * Runtime in-memory cache of the root CA certificate (PEM).
 * Cache RAM của root CA certificate (PEM).
 *
 * Root CA ổn định, sống lâu, là trust anchor; nạp một lần lúc startup, giữ cả trong RAM.
 */
@Component
public class TrustRootCertificateResolver {

    private final AtomicReference<String> rootCertificate = new AtomicReference<>();

    public Optional<String> resolve() {
        return Optional.ofNullable(rootCertificate.get());
    }

    public void update(String rootCert) {
        rootCertificate.set(rootCert);
    }

    public void clear() {
        rootCertificate.set(null);
    }
}
