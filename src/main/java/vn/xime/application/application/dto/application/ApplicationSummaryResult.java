package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Compact read model of an application for list views.
 * Mô hình đọc rút gọn của application cho màn hình danh sách.
 */
public record ApplicationSummaryResult(
        ApplicationId identityId,
        String applicationCode,
        String name,
        String status
) {
}
