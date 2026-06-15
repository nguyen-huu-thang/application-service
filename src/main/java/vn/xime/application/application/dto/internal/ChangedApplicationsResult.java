package vn.xime.application.application.dto.internal;

import java.util.List;

/**
 * Page of changed applications for pull-based sync.
 * Một trang các application thay đổi cho cơ chế đồng bộ pull.
 *
 * maxSequence = change_sequence lớn nhất trong trang (cursor cho lần kéo kế tiếp).
 * hasMore = còn dữ liệu sau trang này hay không.
 */
public record ChangedApplicationsResult(
        List<SubjectInfoResult> applications,
        long maxSequence,
        boolean hasMore
) {
}
