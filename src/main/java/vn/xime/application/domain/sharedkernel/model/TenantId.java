package vn.xime.application.domain.sharedkernel.model;

import java.util.Objects;

/**
 * Tenant identifier (nullable in single-tenant phase).
 * Định danh tenant (hiện chưa multi-tenant nên có thể null ở tầng aggregate).
 *
 * Value object: String chuẩn hóa (trim, không rỗng), tối đa 64 ký tự.
 */
public final class TenantId {

    public static final int MAX_LENGTH = 64;

    private final String value;

    private TenantId(String value) {
        this.value = value;
    }

    public static TenantId of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("tenantId cannot be null");
        }

        String trimmed = raw.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "tenantId must be at most " + MAX_LENGTH + " characters"
            );
        }

        return new TenantId(trimmed);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TenantId other)) {
            return false;
        }
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
