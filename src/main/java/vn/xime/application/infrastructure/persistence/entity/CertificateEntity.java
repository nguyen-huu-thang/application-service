package vn.xime.application.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * JPA entity for the certificates table (this service's runtime mTLS cert from Trust).
 * Entity JPA cho bảng certificates (cert mTLS runtime của service này lấy từ Trust).
 *
 * PK là certificate_id (không phải refresh_token_id): cùng cert -> update token mới;
 * cert mới -> insert mới. Tránh nhân bản cryptographic material khi chỉ rotate token.
 */
@Entity
@Table(
        name = "certificates",
        indexes = {
                @Index(name = "idx_certificates_service_id", columnList = "service_id"),
                @Index(name = "idx_certificates_expires_at", columnList = "expires_at"),
                @Index(name = "idx_certificates_issued_at", columnList = "issued_at")
        }
)
public class CertificateEntity {

    @Id
    @Column(name = "certificate_id", nullable = false, length = 100)
    private String certificateId;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "public_certificate", nullable = false, columnDefinition = "TEXT")
    private String publicCertificate;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "refresh_token_id", nullable = false, length = 100)
    private String refreshTokenId;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // =========================
    // GETTER / SETTER
    // =========================

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getPublicCertificate() {
        return publicCertificate;
    }

    public void setPublicCertificate(String publicCertificate) {
        this.publicCertificate = publicCertificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(String refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
