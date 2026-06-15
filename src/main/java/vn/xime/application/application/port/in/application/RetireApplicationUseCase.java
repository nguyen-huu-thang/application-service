package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.RetireApplicationCommand;

/**
 * Retires (soft-deletes) a disabled application (DISABLED -> RETIRED).
 * Xóa mềm application đã vô hiệu hóa (DISABLED -> RETIRED).
 */
public interface RetireApplicationUseCase {

    void retire(RetireApplicationCommand command);
}
