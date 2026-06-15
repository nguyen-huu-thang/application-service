package vn.xime.application.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.xime.application.infrastructure.persistence.entity.CertificateEntity;

/**
 * Spring Data repository for CertificateEntity (PK = certificate_id).
 * Repository Spring Data cho CertificateEntity (PK = certificate_id).
 */
public interface JpaCertificateRepository extends JpaRepository<CertificateEntity, String> {

    // issued_at <= now && expires_at > now
    List<CertificateEntity> findByIssuedAtLessThanEqualAndExpiresAtAfter(
            Instant issuedAt, Instant expiresAt);

    // primary runtime lookup: mới nhất theo issuedAt rồi certificateId
    Optional<CertificateEntity> findFirstByOrderByIssuedAtDescCertificateIdDesc();

    Optional<CertificateEntity> findByRefreshTokenId(String refreshTokenId);

    void deleteByExpiresAtBefore(Instant now);

    default List<CertificateEntity> findActiveCertificates(Instant now) {
        return findByIssuedAtLessThanEqualAndExpiresAtAfter(now, now);
    }
}
