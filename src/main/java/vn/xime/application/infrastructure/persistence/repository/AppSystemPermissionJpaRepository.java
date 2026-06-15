package vn.xime.application.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.xime.application.infrastructure.persistence.entity.AppSystemPermissionEntity;

/**
 * Spring Data repository for AppSystemPermissionEntity.
 * Repository Spring Data cho AppSystemPermissionEntity.
 */
public interface AppSystemPermissionJpaRepository extends JpaRepository<AppSystemPermissionEntity, Long> {

    @Query("SELECT p FROM AppSystemPermissionEntity p WHERE p.appIdentityId = :appId")
    List<AppSystemPermissionEntity> findByAppIdentityId(@Param("appId") byte[] appId);
}
