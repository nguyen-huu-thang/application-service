package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.ApplicationResult;
import vn.xime.application.application.dto.application.GetApplicationQuery;

/**
 * Fetches one application by identity id or application_code.
 * Lấy một application theo identity id hoặc application_code.
 */
public interface GetApplicationUseCase {

    ApplicationResult get(GetApplicationQuery query);
}
