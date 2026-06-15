package vn.xime.application.application.port.in.application;

import vn.xime.application.application.dto.application.RegisterApplicationCommand;
import vn.xime.application.application.dto.application.RegisterApplicationResult;

/**
 * Registers a new application (status PENDING_REVIEW).
 * Đăng ký application mới (trạng thái PENDING_REVIEW).
 */
public interface RegisterApplicationUseCase {

    RegisterApplicationResult register(RegisterApplicationCommand command);
}
