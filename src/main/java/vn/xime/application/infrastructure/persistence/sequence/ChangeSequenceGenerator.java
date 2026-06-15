package vn.xime.application.infrastructure.persistence.sequence;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Supplies monotonic values for the applications.change_sequence pull cursor.
 * Cấp giá trị monotonic cho cursor pull applications.change_sequence.
 *
 * Dùng Postgres sequence riêng (không BIGSERIAL) để adapter chủ động lấy giá trị mới
 * trên cả insert lẫn update. Sequence được tạo lúc startup; Flyway sẽ chính thức hóa
 * khi schema ổn định (giai đoạn này dùng ddl-auto: update).
 */
@Component
@RequiredArgsConstructor
public class ChangeSequenceGenerator {

    static final String SEQUENCE_NAME = "application_change_sequence";

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureSequenceExists() {
        jdbcTemplate.execute("CREATE SEQUENCE IF NOT EXISTS " + SEQUENCE_NAME);
    }

    /**
     * Returns the next monotonic change_sequence value.
     * Trả về giá trị change_sequence monotonic kế tiếp.
     */
    public long next() {
        Long value = jdbcTemplate.queryForObject(
                "SELECT nextval('" + SEQUENCE_NAME + "')", Long.class);

        if (value == null) {
            throw new IllegalStateException("Failed to obtain next change_sequence");
        }
        return value;
    }
}
