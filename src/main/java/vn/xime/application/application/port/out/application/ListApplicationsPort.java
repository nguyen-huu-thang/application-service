package vn.xime.application.application.port.out.application;

import java.util.List;

import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationStatus;

/**
 * Lists applications with optional status filter and pagination.
 * Liệt kê application với bộ lọc trạng thái tùy chọn và phân trang.
 *
 * statusFilter null = không lọc trạng thái. Bổ sung ngoài danh sách port của design
 * vì ListApplicationsUseCase cần truy vấn trang + đếm tổng.
 */
public interface ListApplicationsPort {

    List<Application> findPage(ApplicationStatus statusFilter, int page, int size);

    long count(ApplicationStatus statusFilter);
}
