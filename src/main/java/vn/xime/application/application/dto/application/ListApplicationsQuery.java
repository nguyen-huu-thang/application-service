package vn.xime.application.application.dto.application;

/**
 * Query to list applications with optional status filter and pagination.
 * Query liệt kê application với bộ lọc trạng thái tùy chọn và phân trang.
 *
 * statusFilter null/blank = không lọc theo trạng thái.
 */
public record ListApplicationsQuery(
        String statusFilter,
        int page,
        int size
) {
}
