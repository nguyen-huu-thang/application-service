package vn.xime.application.application.port.in.internal;

import vn.xime.application.application.dto.internal.GetSubjectInfoQuery;
import vn.xime.application.application.dto.internal.SubjectInfoResult;

/**
 * Resolves subject info for a resource service by identity id (cache miss path).
 * Trả thông tin subject cho resource service theo identity id (đường miss cache).
 */
public interface GetSubjectInfoUseCase {

    SubjectInfoResult get(GetSubjectInfoQuery query);
}
