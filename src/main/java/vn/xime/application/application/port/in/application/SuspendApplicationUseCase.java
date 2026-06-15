package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.SuspendApplicationCommand;

/**
 * Suspends an application (ACTIVE -> SUSPENDED).
 * Tạm khóa application (ACTIVE -> SUSPENDED).
 */
public interface SuspendApplicationUseCase {

    void suspend(SuspendApplicationCommand command);
}
