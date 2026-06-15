package vn.xime.application.application.dto.application;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Query to fetch one application either by identity id or by application_code.
 * Query lấy một application theo identity id hoặc theo application_code.
 *
 * Đúng một trong hai trường được set (build qua factory byId / byCode).
 */
public record GetApplicationQuery(
        ApplicationId identityId,
        String applicationCode
) {

    public static GetApplicationQuery byId(ApplicationId identityId) {
        return new GetApplicationQuery(identityId, null);
    }

    public static GetApplicationQuery byCode(String applicationCode) {
        return new GetApplicationQuery(null, applicationCode);
    }

    public boolean hasId() {
        return identityId != null;
    }
}
