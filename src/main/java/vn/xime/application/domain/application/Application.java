package vn.xime.application.domain.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import vn.xime.application.domain.application.event.ApplicationActivatedEvent;
import vn.xime.application.domain.application.event.ApplicationDisabledEvent;
import vn.xime.application.domain.application.event.ApplicationReactivatedEvent;
import vn.xime.application.domain.application.event.ApplicationRegisteredEvent;
import vn.xime.application.domain.application.event.ApplicationRetiredEvent;
import vn.xime.application.domain.application.event.ApplicationSuspendedEvent;
import vn.xime.application.domain.application.exception.InvalidStatusTransitionException;
import vn.xime.application.domain.permission.PermissionCode;
import vn.xime.application.domain.permission.SystemPermission;
import vn.xime.application.domain.permission.event.SystemPermissionGrantedEvent;
import vn.xime.application.domain.permission.event.SystemPermissionRevokedEvent;
import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;
import vn.xime.application.domain.sharedkernel.model.TenantId;

/**
 * Aggregate root for an APPLICATION subject: registry, lifecycle, system permissions.
 * Aggregate root cho subject loại APPLICATION: registry, vòng đời, quyền hệ thống.
 *
 * Immutable style: mọi thao tác thay đổi state trả về một Application MỚI thay vì mutate.
 * Toàn bộ rule/invariant/state transition nằm tại đây (không để ở use case).
 *
 * Domain events do aggregate raise được mang theo instance kết quả; use case gọi
 * pullDomainEvents() sau khi save để lấy ra dispatch.
 */
public final class Application {

    private final ApplicationId id;
    private final ApplicationCode code;
    private final ApplicationName name;
    private final ApplicationDescription description;
    private final ApplicationStatus status;

    // stateVersion: tăng mỗi khi status hoặc permission thay đổi - dùng làm pull cursor phía consumer.
    private final long stateVersion;

    // changeSequence: monotonic global cursor, do persistence (DB BIGSERIAL) gán. 0 khi chưa lưu.
    private final long changeSequence;

    // tenantId: nullable trong giai đoạn single-tenant.
    private final TenantId tenantId;

    private final List<SystemPermission> permissions;

    private final Instant createdAt;
    private final Instant updatedAt;

    // Domain events do thao tác tạo ra instance này raise (transient, không thuộc state).
    private final List<DomainEvent> domainEvents;

    private Application(
            ApplicationId id,
            ApplicationCode code,
            ApplicationName name,
            ApplicationDescription description,
            ApplicationStatus status,
            long stateVersion,
            long changeSequence,
            TenantId tenantId,
            List<SystemPermission> permissions,
            Instant createdAt,
            Instant updatedAt,
            List<DomainEvent> domainEvents
    ) {
        this.id = Objects.requireNonNull(id, "id is required");
        this.code = Objects.requireNonNull(code, "code is required");
        this.name = Objects.requireNonNull(name, "name is required");
        this.description = description == null ? ApplicationDescription.empty() : description;
        this.status = Objects.requireNonNull(status, "status is required");
        this.stateVersion = stateVersion;
        this.changeSequence = changeSequence;
        this.tenantId = tenantId;
        this.permissions = List.copyOf(permissions);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.domainEvents = new ArrayList<>(domainEvents);
    }

    // =========================
    // FACTORY
    // =========================

    /**
     * Registers a brand-new application in PENDING_REVIEW.
     * Đăng ký application mới ở trạng thái PENDING_REVIEW.
     */
    public static Application register(
            ApplicationId id,
            ApplicationCode code,
            ApplicationName name,
            ApplicationDescription description,
            TenantId tenantId
    ) {
        Instant now = Instant.now();

        List<DomainEvent> events = List.of(
                new ApplicationRegisteredEvent(id, code.value(), name.value(), now)
        );

        return new Application(
                id, code, name, description,
                ApplicationStatus.PENDING_REVIEW,
                0L, 0L, tenantId,
                List.of(),
                now, now,
                events
        );
    }

    /**
     * Rebuilds an application from persistence (no domain events raised).
     * Dựng lại application từ persistence (không raise domain event).
     */
    public static Application reconstitute(
            ApplicationId id,
            ApplicationCode code,
            ApplicationName name,
            ApplicationDescription description,
            ApplicationStatus status,
            long stateVersion,
            long changeSequence,
            TenantId tenantId,
            List<SystemPermission> permissions,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Application(
                id, code, name, description, status,
                stateVersion, changeSequence, tenantId,
                permissions,
                createdAt, updatedAt,
                List.of()
        );
    }

    // =========================
    // STATE TRANSITIONS
    // =========================

    public Application activate() {
        requireStatus(ApplicationStatus.ACTIVE, ApplicationStatus.PENDING_REVIEW);
        Instant now = Instant.now();
        return next(ApplicationStatus.ACTIVE, this.permissions, now,
                new ApplicationActivatedEvent(this.id, now));
    }

    public Application suspend() {
        requireStatus(ApplicationStatus.SUSPENDED, ApplicationStatus.ACTIVE);
        Instant now = Instant.now();
        return next(ApplicationStatus.SUSPENDED, this.permissions, now,
                new ApplicationSuspendedEvent(this.id, now));
    }

    public Application reactivate() {
        requireStatus(ApplicationStatus.ACTIVE, ApplicationStatus.SUSPENDED);
        Instant now = Instant.now();
        return next(ApplicationStatus.ACTIVE, this.permissions, now,
                new ApplicationReactivatedEvent(this.id, now));
    }

    public Application disable() {
        if (this.status != ApplicationStatus.ACTIVE && this.status != ApplicationStatus.SUSPENDED) {
            throw new InvalidStatusTransitionException(this.status, ApplicationStatus.DISABLED);
        }
        Instant now = Instant.now();
        return next(ApplicationStatus.DISABLED, this.permissions, now,
                new ApplicationDisabledEvent(this.id, now));
    }

    public Application retire() {
        requireStatus(ApplicationStatus.RETIRED, ApplicationStatus.DISABLED);
        Instant now = Instant.now();
        return next(ApplicationStatus.RETIRED, this.permissions, now,
                new ApplicationRetiredEvent(this.id, now));
    }

    // =========================
    // PERMISSIONS
    // =========================

    public boolean hasPermission(PermissionCode permission) {
        return permissions.stream().anyMatch(p -> p.permission() == permission);
    }

    public Application grantPermission(PermissionCode permission) {
        if (hasPermission(permission)) {
            throw new vn.xime.application.domain.permission.exception
                    .PermissionAlreadyGrantedException(permission);
        }
        Instant now = Instant.now();

        List<SystemPermission> updated = new ArrayList<>(this.permissions);
        updated.add(new SystemPermission(this.id, permission));

        return next(this.status, updated, now,
                new SystemPermissionGrantedEvent(this.id, permission, now));
    }

    public Application revokePermission(PermissionCode permission) {
        if (!hasPermission(permission)) {
            throw new vn.xime.application.domain.permission.exception
                    .PermissionNotGrantedException(permission);
        }
        Instant now = Instant.now();

        List<SystemPermission> updated = new ArrayList<>(this.permissions);
        updated.removeIf(p -> p.permission() == permission);

        return next(this.status, updated, now,
                new SystemPermissionRevokedEvent(this.id, permission, now));
    }

    // =========================
    // DOMAIN EVENTS
    // =========================

    /**
     * Pulls and clears domain events accumulated on this instance.
     * Lấy ra và xóa các domain event đã tích lũy trên instance này.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(this.domainEvents);
        this.domainEvents.clear();
        return pulled;
    }

    // =========================
    // INTERNAL HELPERS
    // =========================

    private void requireStatus(ApplicationStatus target, ApplicationStatus required) {
        if (this.status != required) {
            throw new InvalidStatusTransitionException(this.status, target);
        }
    }

    private Application next(
            ApplicationStatus newStatus,
            List<SystemPermission> newPermissions,
            Instant now,
            DomainEvent event
    ) {
        return new Application(
                this.id, this.code, this.name, this.description,
                newStatus,
                this.stateVersion + 1,
                this.changeSequence,
                this.tenantId,
                newPermissions,
                this.createdAt, now,
                List.of(event)
        );
    }

    // =========================
    // QUERIES
    // =========================

    public boolean isActive() {
        return status == ApplicationStatus.ACTIVE;
    }

    // =========================
    // GETTERS
    // =========================

    public ApplicationId getId() {
        return id;
    }

    public ApplicationCode getCode() {
        return code;
    }

    public ApplicationName getName() {
        return name;
    }

    public ApplicationDescription getDescription() {
        return description;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public long getChangeSequence() {
        return changeSequence;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public List<SystemPermission> getPermissions() {
        return Collections.unmodifiableList(permissions);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
