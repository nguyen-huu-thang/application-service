package vn.xime.application.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import vn.xime.application.application.port.out.application.CheckApplicationCodeExistsPort;
import vn.xime.application.application.port.out.application.ListApplicationsPort;
import vn.xime.application.application.port.out.application.LoadApplicationByCodePort;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.LoadChangedApplicationsPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;
import vn.xime.application.domain.application.ApplicationStatus;
import vn.xime.application.domain.permission.SystemPermission;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.infrastructure.persistence.entity.AppSystemPermissionEntity;
import vn.xime.application.infrastructure.persistence.entity.ApplicationEntity;
import vn.xime.application.infrastructure.persistence.mapper.ApplicationMapper;
import vn.xime.application.infrastructure.persistence.sequence.ChangeSequenceGenerator;

/**
 * JPA adapter implementing all application out-ports (load/save/check/changed/list).
 * Adapter JPA hiện thực toàn bộ out-port của application (load/save/check/changed/list).
 *
 * - save() cấp lại change_sequence từ ChangeSequenceGenerator mỗi lần lưu, rồi đồng bộ
 *   bảng permission theo diff (chỉ insert phần thiếu / delete phần thừa).
 * - list view truyền permission rỗng (summary không dùng permission) để tránh N+1.
 */
@Repository
@RequiredArgsConstructor
public class JpaApplicationRepositoryAdapter implements
        LoadApplicationPort,
        LoadApplicationByCodePort,
        SaveApplicationPort,
        CheckApplicationCodeExistsPort,
        LoadChangedApplicationsPort,
        ListApplicationsPort {

    private final ApplicationJpaRepository applicationRepo;
    private final AppSystemPermissionJpaRepository permissionRepo;
    private final ChangeSequenceGenerator changeSequenceGenerator;

    // =========================
    // LOAD
    // =========================

    @Override
    public Optional<Application> findById(ApplicationId id) {
        return applicationRepo.findByIdentityId(id.toBytes())
                .map(this::toDomainWithPermissions);
    }

    @Override
    public Optional<Application> findByCode(ApplicationCode code) {
        return applicationRepo.findByApplicationCode(code.value())
                .map(this::toDomainWithPermissions);
    }

    // =========================
    // SAVE
    // =========================

    @Override
    public Application save(Application application) {
        byte[] idBytes = application.getId().toBytes();

        ApplicationEntity entity = ApplicationMapper.toEntity(application);
        entity.setChangeSequence(changeSequenceGenerator.next());

        ApplicationEntity saved = applicationRepo.save(entity);

        syncPermissions(application);

        List<AppSystemPermissionEntity> perms = permissionRepo.findByAppIdentityId(idBytes);
        return ApplicationMapper.toDomain(saved, perms);
    }

    // =========================
    // CHECK
    // =========================

    @Override
    public boolean existsByCode(ApplicationCode code) {
        return applicationRepo.existsByApplicationCode(code.value());
    }

    // =========================
    // CHANGED (pull sync)
    // =========================

    @Override
    public List<Application> findByChangeSequenceAfter(long afterSequence, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return applicationRepo
                .findByChangeSequenceGreaterThanOrderByChangeSequenceAsc(afterSequence, pageable)
                .stream()
                .map(this::toDomainWithPermissions)
                .toList();
    }

    // =========================
    // LIST
    // =========================

    @Override
    public List<Application> findPage(ApplicationStatus statusFilter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<ApplicationEntity> entities = statusFilter == null
                ? applicationRepo.findAll(pageable).getContent()
                : applicationRepo.findByStatus(statusFilter.name(), pageable).getContent();

        // summary không dùng permission -> map với permission rỗng để tránh N+1.
        return entities.stream()
                .map(e -> ApplicationMapper.toDomain(e, List.of()))
                .toList();
    }

    @Override
    public long count(ApplicationStatus statusFilter) {
        return statusFilter == null
                ? applicationRepo.count()
                : applicationRepo.countByStatus(statusFilter.name());
    }

    // =========================
    // HELPERS
    // =========================

    private Application toDomainWithPermissions(ApplicationEntity entity) {
        List<AppSystemPermissionEntity> perms =
                permissionRepo.findByAppIdentityId(entity.getIdentityId());
        return ApplicationMapper.toDomain(entity, perms);
    }

    /**
     * Syncs app_system_permissions to match the aggregate: insert missing, delete removed.
     * Đồng bộ app_system_permissions khớp aggregate: thêm phần thiếu, xóa phần thừa.
     */
    private void syncPermissions(Application application) {
        byte[] idBytes = application.getId().toBytes();

        List<AppSystemPermissionEntity> existing = permissionRepo.findByAppIdentityId(idBytes);

        Set<String> existingNames = new HashSet<>();
        for (AppSystemPermissionEntity e : existing) {
            existingNames.add(e.getPermission());
        }

        Set<String> desiredNames = new HashSet<>();
        for (SystemPermission p : application.getPermissions()) {
            desiredNames.add(p.permission().name());
        }

        // insert phần thiếu
        Instant now = Instant.now();
        List<AppSystemPermissionEntity> toInsert = application.getPermissions().stream()
                .filter(p -> !existingNames.contains(p.permission().name()))
                .map(p -> ApplicationMapper.toPermissionEntity(
                        application.getId(), p.permission(), now))
                .toList();
        if (!toInsert.isEmpty()) {
            permissionRepo.saveAll(toInsert);
        }

        // delete phần thừa
        List<AppSystemPermissionEntity> toDelete = existing.stream()
                .filter(e -> !desiredNames.contains(e.getPermission()))
                .toList();
        if (!toDelete.isEmpty()) {
            permissionRepo.deleteAll(toDelete);
        }
    }
}
