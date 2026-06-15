package vn.xime.application.application.dto.application;

import java.util.List;

/**
 * Generic paginated result wrapper.
 * Bao kết quả phân trang dùng chung.
 */
public record PageResult<T>(
        List<T> items,
        long total
) {
}
