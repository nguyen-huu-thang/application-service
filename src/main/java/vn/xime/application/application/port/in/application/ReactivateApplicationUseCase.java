package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.ReactivateApplicationCommand;

/**
 * Reactivates a suspended application (SUSPENDED -> ACTIVE).
 * Mở lại application đang tạm khóa (SUSPENDED -> ACTIVE).
 */
public interface ReactivateApplicationUseCase {

    void reactivate(ReactivateApplicationCommand command);
}
