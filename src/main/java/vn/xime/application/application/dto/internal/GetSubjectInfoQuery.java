package vn.xime.application.application.dto.internal;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Query for direct subject lookup by identity id (used on cache miss).
 * Query tra cứu subject trực tiếp theo identity id (dùng khi miss cache).
 */
public record GetSubjectInfoQuery(
        ApplicationId identityId
) {
}
