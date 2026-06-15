package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.DisableApplicationCommand;

/**
 * Permanently disables an application (ACTIVE|SUSPENDED -> DISABLED).
 * Vô hiệu hóa vĩnh viễn application (ACTIVE|SUSPENDED -> DISABLED).
 */
public interface DisableApplicationUseCase {

    void disable(DisableApplicationCommand command);
}
