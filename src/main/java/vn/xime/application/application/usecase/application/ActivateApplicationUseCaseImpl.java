package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.ActivateApplicationCommand;
import vn.xime.application.application.port.in.application.ActivateApplicationUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Orchestrates activating an application: load -> domain.activate() -> save -> dispatch.
 * Điều phối kích hoạt application: load -> domain.activate() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class ActivateApplicationUseCaseImpl implements ActivateApplicationUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void activate(ActivateApplicationCommand command) {

        Application app = loadPort.findById(command.identityId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.identityId().toHex()));

        Application activated = app.activate();

        Application saved = savePort.save(activated);
        eventDispatcher.dispatch(saved, activated.pullDomainEvents());
    }
}
