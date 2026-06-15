package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.ApplicationSummaryResult;
import vn.xime.application.application.dto.application.ListApplicationsQuery;
import vn.xime.application.application.dto.application.PageResult;

/**
 * Lists applications with optional status filter and pagination.
 * Liệt kê application với bộ lọc trạng thái tùy chọn và phân trang.
 */
public interface ListApplicationsUseCase {

    PageResult<ApplicationSummaryResult> list(ListApplicationsQuery query);
}
