package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Result of registering an application.
 * Kết quả đăng ký application.
 */
public record RegisterApplicationResult(
        ApplicationId identityId,
        String applicationCode
) {
}
