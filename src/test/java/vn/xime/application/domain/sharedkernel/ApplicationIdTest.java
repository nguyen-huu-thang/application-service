package vn.xime.application.domain.sharedkernel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import vn.xime.application.domain.sharedkernel.factory.ApplicationIdFactory;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

class ApplicationIdTest {

    @Test
    void factory_generates24Bytes() {
        ApplicationId id = ApplicationIdFactory.generate();
        assertThat(id.toBytes()).hasSize(24);
    }

    @Test
    void rejectsWrongLength() {
        assertThatThrownBy(() -> new ApplicationId(new byte[20]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApplicationId(new byte[25]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new ApplicationId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityByContent() {
        byte[] raw = ApplicationIdFactory.generate().toBytes();
        assertThat(new ApplicationId(raw)).isEqualTo(new ApplicationId(raw));
        assertThat(new ApplicationId(raw).hashCode())
                .isEqualTo(new ApplicationId(raw).hashCode());
    }

    @Test
    void toBytes_returnsDefensiveCopy() {
        ApplicationId id = ApplicationIdFactory.generate();
        byte[] bytes = id.toBytes();
        bytes[0] = (byte) (bytes[0] + 1);
        assertThat(id.toBytes()).isNotEqualTo(bytes);
    }

    @Test
    void toHex_is48Chars() {
        ApplicationId id = ApplicationIdFactory.generate();
        assertThat(id.toHex()).hasSize(48).matches("[0-9a-f]+");
    }
}
