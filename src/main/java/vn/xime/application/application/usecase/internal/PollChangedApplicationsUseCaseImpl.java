package vn.xime.application.application.usecase.internal;

import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.internal.ChangedApplicationsResult;
import vn.xime.application.application.dto.internal.PollChangedApplicationsQuery;
import vn.xime.application.application.dto.internal.SubjectInfoResult;
import vn.xime.application.application.mapper.ApplicationResultMapper;
import vn.xime.application.application.port.in.internal.PollChangedApplicationsUseCase;
import vn.xime.application.application.port.out.application.LoadChangedApplicationsPort;
import vn.xime.application.domain.application.Application;

/**
 * Pulls applications changed after a change_sequence cursor for pull-based sync (read-only).
 * Kéo các application thay đổi sau con trỏ change_sequence cho đồng bộ pull (chỉ đọc).
 */
@RequiredArgsConstructor
public class PollChangedApplicationsUseCaseImpl implements PollChangedApplicationsUseCase {

    private static final int MAX_LIMIT = 200;

    private final LoadChangedApplicationsPort loadChangedPort;

    @Override
    @Transactional(readOnly = true)
    public ChangedApplicationsResult poll(PollChangedApplicationsQuery query) {

        int limit = normalizeLimit(query.limit());

        List<Application> apps =
                loadChangedPort.findByChangeSequenceAfter(query.afterSequence(), limit);

        List<SubjectInfoResult> results = apps.stream()
                .map(ApplicationResultMapper::toSubjectInfo)
                .toList();

        // apps sắp xếp tăng dần theo change_sequence -> phần tử cuối mang sequence lớn nhất.
        long maxSequence = apps.isEmpty()
                ? query.afterSequence()
                : apps.get(apps.size() - 1).getChangeSequence();

        boolean hasMore = apps.size() == limit;

        return new ChangedApplicationsResult(results, maxSequence, hasMore);
    }

    private static int normalizeLimit(int limit) {
        if (limit <= 0) {
            return MAX_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
