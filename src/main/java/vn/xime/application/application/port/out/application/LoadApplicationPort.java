package vn.xime.application.application.port.out.application;

import java.util.Optional;

import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Loads an application aggregate by its identity id.
 * Tải aggregate application theo identity id.
 */
public interface LoadApplicationPort {

    Optional<Application> findById(ApplicationId id);
}
