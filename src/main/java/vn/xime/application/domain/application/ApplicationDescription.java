package vn.xime.application.domain.application;

import java.util.Objects;

/**
 * Value object: optional description of an application (nullable, max 2000).
 * Value object: mô tả tùy chọn của application (cho phép null, tối đa 2000).
 *
 * Dùng of(null) hoặc chuỗi rỗng để biểu diễn "không có mô tả" (value == null).
 */
public final class ApplicationDescription {

    public static final int MAX_LENGTH = 2000;

    private static final ApplicationDescription EMPTY = new ApplicationDescription(null);

    private final String value;

    private ApplicationDescription(String value) {
        this.value = value;
    }

    public static ApplicationDescription of(String raw) {
        if (raw == null) {
            return EMPTY;
        }

        String trimmed = raw.trim();

        if (trimmed.isEmpty()) {
            return EMPTY;
        }

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "description must be at most " + MAX_LENGTH + " characters"
            );
        }

        return new ApplicationDescription(trimmed);
    }

    public static ApplicationDescription empty() {
        return EMPTY;
    }

    public boolean isPresent() {
        return value != null;
    }

    /** Giá trị mô tả, có thể null khi không có mô tả. */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationDescription other)) {
            return false;
        }
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value == null ? "" : value;
    }
}
