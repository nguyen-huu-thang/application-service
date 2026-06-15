package vn.xime.application.application.usecase.application;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.ApplicationSummaryResult;
import vn.xime.application.application.dto.application.ListApplicationsQuery;
import vn.xime.application.application.dto.application.PageResult;
import vn.xime.application.application.mapper.ApplicationResultMapper;
import vn.xime.application.application.port.in.application.ListApplicationsUseCase;
import vn.xime.application.application.port.out.application.ListApplicationsPort;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationStatus;

/**
 * Lists applications with optional status filter and pagination (read-only).
 * Liệt kê application với bộ lọc trạng thái tùy chọn và phân trang (chỉ đọc).
 */
@RequiredArgsConstructor
public class ListApplicationsUseCaseImpl implements ListApplicationsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final ListApplicationsPort listPort;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ApplicationSummaryResult> list(ListApplicationsQuery query) {

        ApplicationStatus statusFilter = parseStatus(query.statusFilter());
        int page = Math.max(query.page(), 0);
        int size = normalizeSize(query.size());

        List<ApplicationSummaryResult> items = listPort.findPage(statusFilter, page, size).stream()
                .map(ApplicationResultMapper::toSummary)
                .toList();

        long total = listPort.count(statusFilter);

        return new PageResult<>(items, total);
    }

    private static ApplicationStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ApplicationStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid status filter: " + raw);
        }
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
