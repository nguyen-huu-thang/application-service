package vn.xime.application.domain.application;

import java.util.Objects;

/**
 * Value object: display name of an application (trimmed, non-blank, max 255).
 * Value object: tên hiển thị của application (đã trim, không rỗng, tối đa 255).
 */
public final class ApplicationName {

    public static final int MAX_LENGTH = 255;

    private final String value;

    private ApplicationName(String value) {
        this.value = value;
    }

    public static ApplicationName of(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        String trimmed = raw.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        if (trimmed.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "name must be at most " + MAX_LENGTH + " characters"
            );
        }

        return new ApplicationName(trimmed);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationName other)) {
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
