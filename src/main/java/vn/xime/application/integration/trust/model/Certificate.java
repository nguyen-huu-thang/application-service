package vn.xime.application.integration.trust.model;

import java.time.Instant;

/**
 * Runtime certificate of this service (transport/cache object).
 * Certificate runtime của service này (object truyền tải/cache).
 *
 * Dùng trong: bootstrap flow, certificate rotation flow, runtime trust update.
 */
public record Certificate(

        String certificateId,

        String publicCertificate,

        String privateKey,

        String serviceId,

        String refreshTokenId,

        String refreshToken,

        Instant issuedAt,

        Instant expiresAt
) {
}
