package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.ReactivateApplicationCommand;
import vn.xime.application.application.port.in.application.ReactivateApplicationUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Orchestrates reactivating an application: load -> domain.reactivate() -> save -> dispatch.
 * Điều phối mở lại application: load -> domain.reactivate() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class ReactivateApplicationUseCaseImpl implements ReactivateApplicationUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void reactivate(ReactivateApplicationCommand command) {

        Application app = loadPort.findById(command.identityId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.identityId().toHex()));

        Application reactivated = app.reactivate();

        Application saved = savePort.save(reactivated);
        eventDispatcher.dispatch(saved, reactivated.pullDomainEvents());
    }
}
