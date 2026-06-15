package vn.xime.application.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.xime.application.infrastructure.persistence.entity.ApplicationEntity;

/**
 * Spring Data repository for ApplicationEntity (PK = identity_id bytes).
 * Repository Spring Data cho ApplicationEntity (PK = identity_id bytes).
 */
public interface ApplicationJpaRepository extends JpaRepository<ApplicationEntity, byte[]> {

    @Query("SELECT a FROM ApplicationEntity a WHERE a.identityId = :id")
    Optional<ApplicationEntity> findByIdentityId(@Param("id") byte[] id);

    Optional<ApplicationEntity> findByApplicationCode(String applicationCode);

    boolean existsByApplicationCode(String applicationCode);

    // pull sync: lấy các app có change_sequence > cursor, tăng dần
    List<ApplicationEntity> findByChangeSequenceGreaterThanOrderByChangeSequenceAsc(
            long afterSequence, Pageable pageable);

    // list + filter
    Page<ApplicationEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
