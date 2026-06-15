package vn.xime.application.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import vn.xime.application.infrastructure.persistence.entity.CertificateEntity;
import vn.xime.application.infrastructure.persistence.mapper.CertificateMapper;
import vn.xime.application.integration.trust.model.Certificate;

/**
 * Repository facade over JpaCertificateRepository working in Certificate records.
 * Facade repository trên JpaCertificateRepository, làm việc bằng record Certificate.
 */
@Repository
public class CertificateRepository {

    private final JpaCertificateRepository repository;

    public CertificateRepository(JpaCertificateRepository repository) {
        this.repository = repository;
    }

    // =========================
    // FIND
    // =========================

    public Optional<Certificate> findLatest() {
        return repository.findFirstByOrderByIssuedAtDescCertificateIdDesc()
                .map(CertificateMapper::toRecord);
    }

    public Optional<Certificate> findById(String certificateId) {
        return repository.findById(certificateId).map(CertificateMapper::toRecord);
    }

    public Optional<Certificate> findByRefreshTokenId(String refreshTokenId) {
        return repository.findByRefreshTokenId(refreshTokenId).map(CertificateMapper::toRecord);
    }

    public List<Certificate> findActiveCertificates(Instant now) {
        return repository.findActiveCertificates(now).stream()
                .map(CertificateMapper::toRecord)
                .toList();
    }

    // =========================
    // SAVE
    // =========================

    public Certificate save(Certificate certificate) {
        CertificateEntity saved = repository.save(CertificateMapper.toEntity(certificate));
        return CertificateMapper.toRecord(saved);
    }

    public List<Certificate> saveAll(List<Certificate> certificates) {
        List<CertificateEntity> entities = certificates.stream()
                .map(CertificateMapper::toEntity)
                .toList();
        return repository.saveAll(entities).stream()
                .map(CertificateMapper::toRecord)
                .toList();
    }

    // =========================
    // DELETE
    // =========================

    public void deleteExpiredCertificates(Instant now) {
        repository.deleteByExpiresAtBefore(now);
    }

    public void deleteAll() {
        repository.deleteAll();
    }
}
