package vn.xime.application.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;
import vn.xime.application.domain.application.ApplicationDescription;
import vn.xime.application.domain.application.ApplicationName;
import vn.xime.application.domain.application.ApplicationStatus;
import vn.xime.application.domain.permission.PermissionCode;
import vn.xime.application.domain.permission.SystemPermission;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.domain.sharedkernel.model.TenantId;
import vn.xime.application.infrastructure.persistence.entity.AppSystemPermissionEntity;
import vn.xime.application.infrastructure.persistence.entity.ApplicationEntity;

/**
 * Maps between the Application aggregate and JPA entities, unwrapping value objects.
 * Map giữa aggregate Application và entity JPA, mở bọc value object.
 *
 * change_sequence trên entity do adapter cấp lại từ sequence; mapper chỉ chép giá trị
 * hiện có của domain (entity->domain dựng lại đúng giá trị đã lưu).
 */
public final class ApplicationMapper {

    private ApplicationMapper() {
    }

    // =========================
    // Domain -> Entity
    // =========================

    public static ApplicationEntity toEntity(Application app) {
        if (app == null) {
            throw new IllegalArgumentException("Application must not be null");
        }

        ApplicationEntity e = new ApplicationEntity();

        e.setIdentityId(copy(app.getId().toBytes()));
        e.setApplicationCode(app.getCode().value());
        e.setName(app.getName().value());
        e.setDescription(app.getDescription().value());
        e.setStatus(app.getStatus().name());
        e.setStateVersion(app.getStateVersion());
        e.setChangeSequence(app.getChangeSequence());
        e.setTenantId(app.getTenantId() == null ? null : app.getTenantId().value());
        e.setCreatedAt(app.getCreatedAt());
        e.setUpdatedAt(app.getUpdatedAt());

        return e;
    }

    public static AppSystemPermissionEntity toPermissionEntity(
            ApplicationId appId, PermissionCode permission, Instant createdAt) {

        AppSystemPermissionEntity e = new AppSystemPermissionEntity();
        e.setAppIdentityId(copy(appId.toBytes()));
        e.setPermission(permission.name());
        e.setCreatedAt(createdAt);
        return e;
    }

    // =========================
    // Entity -> Domain
    // =========================

    public static Application toDomain(ApplicationEntity e, List<AppSystemPermissionEntity> permissions) {
        if (e == null) {
            throw new IllegalArgumentException("ApplicationEntity must not be null");
        }

        ApplicationId id = new ApplicationId(copy(e.getIdentityId()));

        List<SystemPermission> domainPermissions = permissions.stream()
                .map(p -> new SystemPermission(id, PermissionCode.from(p.getPermission())))
                .toList();

        TenantId tenantId = e.getTenantId() == null ? null : TenantId.of(e.getTenantId());

        return Application.reconstitute(
                id,
                ApplicationCode.of(e.getApplicationCode()),
                ApplicationName.of(e.getName()),
                ApplicationDescription.of(e.getDescription()),
                ApplicationStatus.valueOf(e.getStatus()),
                e.getStateVersion(),
                e.getChangeSequence(),
                tenantId,
                domainPermissions,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    // =========================
    // HELPERS
    // =========================

    private static byte[] copy(byte[] src) {
        return src == null ? null : Arrays.copyOf(src, src.length);
    }
}
