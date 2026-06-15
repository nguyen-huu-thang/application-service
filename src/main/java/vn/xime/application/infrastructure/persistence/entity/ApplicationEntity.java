package vn.xime.application.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA entity for the applications table.
 * Entity JPA cho bảng applications.
 *
 * PK là identity_id (KSUID 24 byte, BYTEA). change_sequence là cursor pull sync,
 * giá trị do adapter cấp từ sequence application_change_sequence mỗi lần lưu.
 */
@Entity
@Table(
        name = "applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_applications_code", columnNames = {"application_code"}),
                @UniqueConstraint(name = "uq_applications_change", columnNames = {"change_sequence"})
        },
        indexes = {
                @Index(name = "idx_applications_status", columnList = "status")
        }
)
public class ApplicationEntity {

    @Id
    @Column(name = "identity_id", nullable = false, columnDefinition = "BYTEA")
    private byte[] identityId;

    @Column(name = "application_code", nullable = false, length = 64)
    private String applicationCode;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "state_version", nullable = false)
    private long stateVersion;

    @Column(name = "change_sequence", nullable = false)
    private long changeSequence;

    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // =========================
    // GETTER / SETTER
    // =========================

    public byte[] getIdentityId() {
        return identityId;
    }

    public void setIdentityId(byte[] identityId) {
        this.identityId = identityId;
    }

    public String getApplicationCode() {
        return applicationCode;
    }

    public void setApplicationCode(String applicationCode) {
        this.applicationCode = applicationCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStateVersion() {
        return stateVersion;
    }

    public void setStateVersion(long stateVersion) {
        this.stateVersion = stateVersion;
    }

    public long getChangeSequence() {
        return changeSequence;
    }

    public void setChangeSequence(long changeSequence) {
        this.changeSequence = changeSequence;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
