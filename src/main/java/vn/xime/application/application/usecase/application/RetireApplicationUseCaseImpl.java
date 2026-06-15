package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.RetireApplicationCommand;
import vn.xime.application.application.port.in.application.RetireApplicationUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;

/**
 * Orchestrates retiring an application: load -> domain.retire() -> save -> dispatch.
 * Điều phối xóa mềm application: load -> domain.retire() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class RetireApplicationUseCaseImpl implements RetireApplicationUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void retire(RetireApplicationCommand command) {

        Application app = loadPort.findById(command.identityId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.identityId().toHex()));

        Application retired = app.retire();

        Application saved = savePort.save(retired);
        eventDispatcher.dispatch(saved, retired.pullDomainEvents());
    }
}
