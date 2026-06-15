package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.ApplicationResult;
import vn.xime.application.application.dto.application.GetApplicationQuery;
import vn.xime.application.application.mapper.ApplicationResultMapper;
import vn.xime.application.application.port.in.application.GetApplicationUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationByCodePort;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Fetches one application by id or by code (read-only).
 * Lấy một application theo id hoặc code (chỉ đọc).
 */
@RequiredArgsConstructor
public class GetApplicationUseCaseImpl implements GetApplicationUseCase {

    private final LoadApplicationPort loadPort;
    private final LoadApplicationByCodePort loadByCodePort;

    @Override
    @Transactional(readOnly = true)
    public ApplicationResult get(GetApplicationQuery query) {

        Application app = query.hasId()
                ? loadPort.findById(query.identityId())
                        .orElseThrow(() -> new ApplicationNotFoundException(query.identityId().toHex()))
                : loadByCodePort.findByCode(ApplicationCode.of(query.applicationCode()))
                        .orElseThrow(() -> new ApplicationNotFoundException(query.applicationCode()));

        return ApplicationResultMapper.toResult(app);
    }
}
