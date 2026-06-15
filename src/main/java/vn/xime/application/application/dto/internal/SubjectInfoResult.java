package vn.xime.application.application.dto.internal;

import java.util.List;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Subject snapshot returned to resource services over the internal gRPC API.
 * Ảnh chụp subject trả về cho resource service qua gRPC nội bộ.
 *
 * subjectType luôn là "APPLICATION". tenantId có thể null.
 */
public record SubjectInfoResult(
        ApplicationId identityId,
        String subjectType,
        String status,
        long stateVersion,
        long changeSequence,
        List<String> permissions,
        String tenantId
) {
}
