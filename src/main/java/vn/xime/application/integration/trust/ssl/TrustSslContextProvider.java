package vn.xime.application.integration.trust.ssl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;

import lombok.RequiredArgsConstructor;

import vn.xime.application.integration.trust.cert.TrustCertificateResolver;
import vn.xime.application.integration.trust.model.Certificate;
import vn.xime.application.integration.trust.publicca.TrustRootCertificateResolver;

/**
 * Builds and caches the mTLS client SSL context (for gRPC calls to Trust).
 * Dựng và cache SSL context client mTLS (cho gRPC gọi sang Trust).
 *
 * Trust gửi service cert/private key dạng raw base64 (không PEM header) -> bọc PEM nếu thiếu.
 */
@Component
@RequiredArgsConstructor
public class TrustSslContextProvider {

    private final TrustCertificateResolver trustCertificateResolver;
    private final TrustRootCertificateResolver trustRootCertificateResolver;

    private volatile SslContext sslContext;

    public SslContext getSslContext() {
        SslContext current = sslContext;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (sslContext == null) {
                sslContext = buildSslContext();
            }
            return sslContext;
        }
    }

    public synchronized void reload() {
        sslContext = buildSslContext();
    }

    private SslContext buildSslContext() {
        Certificate certificate = trustCertificateResolver.resolve()
                .orElseThrow(() -> new IllegalStateException("runtime certificate not found"));

        String rootCertificate = trustRootCertificateResolver.resolve()
                .orElseThrow(() -> new IllegalStateException("root certificate not found"));

        try {
            return GrpcSslContexts.forClient()
                    .trustManager(toStream(rootCertificate))
                    .keyManager(
                            certInputStream(certificate.publicCertificate()),
                            privateKeyInputStream(certificate.privateKey()))
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("failed to build ssl context", exception);
        }
    }

    private InputStream toStream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream certInputStream(String value) {
        String pem = value.startsWith("-----")
                ? value
                : "-----BEGIN CERTIFICATE-----\n" + value + "\n-----END CERTIFICATE-----\n";
        return toStream(pem);
    }

    private InputStream privateKeyInputStream(String value) {
        String pem = value.startsWith("-----")
                ? value
                : "-----BEGIN PRIVATE KEY-----\n" + value + "\n-----END PRIVATE KEY-----\n";
        return toStream(pem);
    }
}
