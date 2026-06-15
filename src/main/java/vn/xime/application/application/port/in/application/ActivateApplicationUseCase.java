package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.ActivateApplicationCommand;

/**
 * Activates an application (PENDING_REVIEW -> ACTIVE).
 * Kích hoạt application (PENDING_REVIEW -> ACTIVE).
 */
public interface ActivateApplicationUseCase {

    void activate(ActivateApplicationCommand command);
}
