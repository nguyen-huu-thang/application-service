package vn.xime.application.infrastructure.persistence.mapper;

import vn.xime.application.infrastructure.persistence.entity.CertificateEntity;
import vn.xime.application.integration.trust.model.Certificate;

/**
 * Maps between the runtime Certificate record and CertificateEntity.
 * Map giữa record Certificate runtime và CertificateEntity.
 */
public final class CertificateMapper {

    private CertificateMapper() {
    }

    public static Certificate toRecord(CertificateEntity e) {
        if (e == null) {
            throw new IllegalArgumentException("CertificateEntity must not be null");
        }
        requireNonNull(e.getCertificateId(), "certificateId");
        requireNonNull(e.getServiceId(), "serviceId");
        requireNonNull(e.getPublicCertificate(), "publicCertificate");
        requireNonNull(e.getRefreshTokenId(), "refreshTokenId");
        requireNonNull(e.getIssuedAt(), "issuedAt");
        requireNonNull(e.getExpiresAt(), "expiresAt");

        return new Certificate(
                e.getCertificateId(),
                e.getPublicCertificate(),
                e.getPrivateKey(),
                e.getServiceId(),
                e.getRefreshTokenId(),
                e.getRefreshToken(),
                e.getIssuedAt(),
                e.getExpiresAt());
    }

    public static CertificateEntity toEntity(Certificate r) {
        if (r == null) {
            throw new IllegalArgumentException("Certificate must not be null");
        }
        CertificateEntity e = new CertificateEntity();
        e.setCertificateId(r.certificateId());
        e.setServiceId(r.serviceId());
        e.setPublicCertificate(r.publicCertificate());
        e.setPrivateKey(r.privateKey());
        e.setRefreshTokenId(r.refreshTokenId());
        e.setRefreshToken(r.refreshToken());
        e.setIssuedAt(r.issuedAt());
        e.setExpiresAt(r.expiresAt());
        return e;
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new IllegalStateException(field + " must not be null");
        }
    }
}
