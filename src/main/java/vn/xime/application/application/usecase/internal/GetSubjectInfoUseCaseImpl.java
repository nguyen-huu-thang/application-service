package vn.xime.application.application.usecase.internal;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.internal.GetSubjectInfoQuery;
import vn.xime.application.application.dto.internal.SubjectInfoResult;
import vn.xime.application.application.mapper.ApplicationResultMapper;
import vn.xime.application.application.port.in.internal.GetSubjectInfoUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Resolves subject info by identity id for resource services (read-only).
 * Trả thông tin subject theo identity id cho resource service (chỉ đọc).
 */
@RequiredArgsConstructor
public class GetSubjectInfoUseCaseImpl implements GetSubjectInfoUseCase {

    private final LoadApplicationPort loadPort;

    @Override
    @Transactional(readOnly = true)
    public SubjectInfoResult get(GetSubjectInfoQuery query) {

        Application app = loadPort.findById(query.identityId())
                .orElseThrow(() -> new ApplicationNotFoundException(query.identityId().toHex()));

        return ApplicationResultMapper.toSubjectInfo(app);
    }
}
