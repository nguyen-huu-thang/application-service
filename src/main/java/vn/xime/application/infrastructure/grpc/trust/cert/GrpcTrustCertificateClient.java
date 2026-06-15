package vn.xime.application.infrastructure.grpc.trust.cert;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;

import vn.xime.application.infrastructure.grpc.channel.GrpcChannelProvider;
import vn.xime.application.integration.trust.model.Certificate;
import vn.xime.trust.grpc.external.certificate.CertificateServiceGrpc;
import vn.xime.trust.grpc.external.certificate.RotateCertificateRequest;
import vn.xime.trust.grpc.external.certificate.RotateCertificateResponse;

/**
 * Low-level gRPC transport adapter to Trust Service CertificateService (rotate cert).
 * Adapter transport gRPC mức thấp tới CertificateService của Trust (rotate cert).
 *
 * Chỉ làm transport + map proto; không orchestration, không persistence, không policy.
 * Trust gRPC endpoint lấy từ config trust-service.grpc.* (mặc định localhost:9090).
 */
@Component
public class GrpcTrustCertificateClient {

    private final GrpcChannelProvider channelProvider;
    private final String host;
    private final int port;

    public GrpcTrustCertificateClient(
            GrpcChannelProvider channelProvider,
            @Value("${trust-service.grpc.host:localhost}") String host,
            @Value("${trust-service.grpc.port:9090}") int port) {
        this.channelProvider = channelProvider;
        this.host = host;
        this.port = port;
    }

    public Certificate rotateCertificate(String tokenId, String refreshToken, String privateKey) {
        ManagedChannel channel = channelProvider.getChannel(host, port);

        try {
            CertificateServiceGrpc.CertificateServiceBlockingStub stub =
                    CertificateServiceGrpc.newBlockingStub(channel);

            RotateCertificateRequest request = RotateCertificateRequest.newBuilder()
                    .setTokenId(tokenId)
                    .setRefreshToken(refreshToken)
                    .setPrivateKey(privateKey)
                    .build();

            RotateCertificateResponse response = stub.rotateCertificate(request);

            return mapResponse(response);
        } catch (StatusRuntimeException exception) {
            throw new RuntimeException("trust-service certificate rotation failed", exception);
        }
    }

    private Certificate mapResponse(RotateCertificateResponse response) {
        return new Certificate(
                response.getCertificate().getId(),
                response.getCertificate().getPublicCert(),
                response.getCertificate().getPrivateKey(),
                response.getServiceId(),
                response.getRefreshTokenId(),
                response.getNextRefreshToken(),
                Instant.ofEpochMilli(response.getIssuedAt()),
                Instant.ofEpochMilli(response.getExpiresAt()));
    }
}
