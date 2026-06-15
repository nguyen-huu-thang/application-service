package vn.xime.application.application.port.out.application;

import java.util.Optional;

import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;

/**
 * Loads an application aggregate by its normalized application_code.
 * Tải aggregate application theo application_code đã chuẩn hóa.
 */
public interface LoadApplicationByCodePort {

    Optional<Application> findByCode(ApplicationCode code);
}
