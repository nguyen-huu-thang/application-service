package vn.xime.application.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA entity for the app_system_permissions table.
 * Entity JPA cho bảng app_system_permissions.
 *
 * Quản lý tách rời aggregate (không quan hệ JPA) - adapter tự đồng bộ diff khi lưu
 * để kiểm soát chính xác insert/delete và tránh xung đột unique (app_identity_id, permission).
 */
@Entity
@Table(
        name = "app_system_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_app_perm",
                        columnNames = {"app_identity_id", "permission"}
                )
        },
        indexes = {
                @Index(name = "idx_perm_app_id", columnList = "app_identity_id")
        }
)
public class AppSystemPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "app_identity_id", nullable = false, columnDefinition = "BYTEA")
    private byte[] appIdentityId;

    @Column(name = "permission", nullable = false, length = 128)
    private String permission;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // =========================
    // GETTER / SETTER
    // =========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public byte[] getAppIdentityId() {
        return appIdentityId;
    }

    public void setAppIdentityId(byte[] appIdentityId) {
        this.appIdentityId = appIdentityId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
