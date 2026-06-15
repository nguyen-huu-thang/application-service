package vn.xime.application.infrastructure.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.grpc.ServerCredentials;
import io.grpc.TlsServerCredentials;

import lombok.RequiredArgsConstructor;

import vn.xime.application.integration.trust.cert.TrustCertificateResolver;
import vn.xime.application.integration.trust.model.Certificate;
import vn.xime.application.integration.trust.publicca.TrustRootCertificateResolver;

/**
 * Builds TlsServerCredentials (mTLS, clientAuth REQUIRE) for the custom gRPC server.
 * Dựng TlsServerCredentials (mTLS, clientAuth REQUIRE) cho gRPC server tùy chỉnh.
 *
 * Đọc cert của chính service từ TrustCertificateResolver (nạp từ Trust lúc bootstrap) và
 * root CA từ TrustRootCertificateResolver. Gọi một lần khi tạo Server bean (sau cert loaders).
 * Lý do NettyServerBuilder tùy chỉnh: cert ở in-memory (từ Trust), không phải file PEM.
 */
@Component
@RequiredArgsConstructor
public class GrpcServerCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerCredentialsProvider.class);

    private final TrustCertificateResolver trustCertificateResolver;
    private final TrustRootCertificateResolver trustRootCertificateResolver;

    public ServerCredentials buildServerCredentials() throws IOException {
        log.info("Building gRPC server mTLS credentials");

        Certificate certificate = trustCertificateResolver.resolve()
                .orElseThrow(() -> new IllegalStateException(
                        "Runtime certificate not found - cert loader has not run yet"));

        String rootCaPem = trustRootCertificateResolver.resolve()
                .orElseThrow(() -> new IllegalStateException(
                        "Root CA certificate not found - root cert loader has not run yet"));

        ServerCredentials credentials = TlsServerCredentials.newBuilder()
                .keyManager(
                        certInputStream(certificate.publicCertificate()),
                        privateKeyInputStream(certificate.privateKey()))
                .trustManager(toStream(rootCaPem))
                .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                .build();

        log.info("gRPC server mTLS credentials built successfully (clientAuth=REQUIRE)");
        return credentials;
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

    private InputStream toStream(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
