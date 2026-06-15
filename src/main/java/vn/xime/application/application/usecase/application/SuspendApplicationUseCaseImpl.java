package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.SuspendApplicationCommand;
import vn.xime.application.application.port.in.application.SuspendApplicationUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Orchestrates suspending an application: load -> domain.suspend() -> save -> dispatch.
 * Điều phối tạm khóa application: load -> domain.suspend() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class SuspendApplicationUseCaseImpl implements SuspendApplicationUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void suspend(SuspendApplicationCommand command) {

        Application app = loadPort.findById(command.identityId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.identityId().toHex()));

        Application suspended = app.suspend();

        Application saved = savePort.save(suspended);
        eventDispatcher.dispatch(saved, suspended.pullDomainEvents());
    }
}
