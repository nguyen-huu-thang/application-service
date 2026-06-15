package vn.xime.application.domain.application;

import java.util.Objects;

import vn.xime.application.domain.sharedkernel.service.ApplicationCodeNormalizer;

/**
 * Value object wrapping a normalized application_code.
 * Value object bọc application_code đã chuẩn hóa.
 *
 * Mọi instance đều đã normalize (lowercase + trim + validate) qua
 * ApplicationCodeNormalizer, nên hai code "Xime-Social" và "xime-social" bằng nhau.
 */
public final class ApplicationCode {

    private final String value;

    private ApplicationCode(String value) {
        this.value = value;
    }

    public static ApplicationCode of(String raw) {
        return new ApplicationCode(ApplicationCodeNormalizer.normalize(raw));
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationCode other)) {
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
