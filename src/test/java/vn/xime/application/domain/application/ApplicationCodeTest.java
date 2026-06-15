package vn.xime.application.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ApplicationCodeTest {

    @Test
    void of_normalizesLowercaseAndTrim() {
        assertThat(ApplicationCode.of("  Xime-Social  ").value()).isEqualTo("xime-social");
    }

    @Test
    void of_equalityByNormalizedValue() {
        assertThat(ApplicationCode.of("Xime-Social")).isEqualTo(ApplicationCode.of("xime-social"));
    }

    @Test
    void of_allowsInternalHyphenAndDigits() {
        assertThat(ApplicationCode.of("xime-social-2").value()).isEqualTo("xime-social-2");
    }

    @Test
    void of_rejectsLeadingHyphen() {
        assertThatThrownBy(() -> ApplicationCode.of("-xime"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_rejectsTrailingHyphen() {
        assertThatThrownBy(() -> ApplicationCode.of("xime-"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_rejectsTooShort() {
        assertThatThrownBy(() -> ApplicationCode.of("a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_rejectsIllegalCharacters() {
        assertThatThrownBy(() -> ApplicationCode.of("xime_social"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApplicationCode.of("xime social"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void of_rejectsNull() {
        assertThatThrownBy(() -> ApplicationCode.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== ApplicationName =====

    @Test
    void name_trimsAndRejectsBlank() {
        assertThat(ApplicationName.of("  Hello  ").value()).isEqualTo("Hello");
        assertThatThrownBy(() -> ApplicationName.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== ApplicationDescription =====

    @Test
    void description_nullBecomesEmpty() {
        ApplicationDescription d = ApplicationDescription.of(null);
        assertThat(d.isPresent()).isFalse();
        assertThat(d.value()).isNull();
    }

    @Test
    void description_blankBecomesEmpty() {
        assertThat(ApplicationDescription.of("   ").isPresent()).isFalse();
    }

    @Test
    void description_keepsTrimmedValue() {
        ApplicationDescription d = ApplicationDescription.of("  hi  ");
        assertThat(d.isPresent()).isTrue();
        assertThat(d.value()).isEqualTo("hi");
    }
}
