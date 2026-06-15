package vn.xime.application.application.port.out.application;

import vn.xime.application.domain.application.ApplicationCode;

/**
 * Checks whether an application_code is already taken.
 * Kiểm tra application_code đã được dùng hay chưa.
 */
public interface CheckApplicationCodeExistsPort {

    boolean existsByCode(ApplicationCode code);
}
