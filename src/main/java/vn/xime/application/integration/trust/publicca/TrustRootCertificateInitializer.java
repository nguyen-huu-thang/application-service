package vn.xime.application.integration.trust.publicca;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import vn.xime.application.infrastructure.security.store.RootCertificateFileStore;

/**
 * Loads the root CA from filesystem into the runtime RAM cache at startup.
 * Nạp root CA từ filesystem vào cache RAM lúc startup.
 *
 * Root CA là trust anchor ổn định -> nạp một lần. Lỗi nạp = fail startup (không có trust anchor).
 */
@Component
@RequiredArgsConstructor
public class TrustRootCertificateInitializer {

    private final RootCertificateFileStore rootCertificateFileStore;
    private final TrustRootCertificateResolver trustRootCertificateResolver;

    public void initialize() {
        try {
            String rootCertificate = rootCertificateFileStore.load();
            trustRootCertificateResolver.update(rootCertificate);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to initialize root certificate", exception);
        }
    }
}
