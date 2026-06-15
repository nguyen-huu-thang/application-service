package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.DisableApplicationCommand;
import vn.xime.application.application.port.in.application.DisableApplicationUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Orchestrates disabling an application: load -> domain.disable() -> save -> dispatch.
 * Điều phối vô hiệu hóa application: load -> domain.disable() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class DisableApplicationUseCaseImpl implements DisableApplicationUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void disable(DisableApplicationCommand command) {

        Application app = loadPort.findById(command.identityId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.identityId().toHex()));

        Application disabled = app.disable();

        Application saved = savePort.save(disabled);
        eventDispatcher.dispatch(saved, disabled.pullDomainEvents());
    }
}
