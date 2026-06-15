package vn.xime.application.application.usecase.permission;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.permission.GrantSystemPermissionCommand;
import vn.xime.application.application.port.in.permission.GrantSystemPermissionUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;
import vn.xime.application.domain.permission.PermissionCode;

/**
 * Orchestrates granting a permission: parse code -> load -> domain.grant() -> save -> dispatch.
 * Điều phối cấp quyền: phân giải code -> load -> domain.grant() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class GrantSystemPermissionUseCaseImpl implements GrantSystemPermissionUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void grant(GrantSystemPermissionCommand command) {

        PermissionCode permission = PermissionCode.from(command.permission());

        Application app = loadPort.findById(command.appId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.appId().toHex()));

        Application updated = app.grantPermission(permission);

        Application saved = savePort.save(updated);
        eventDispatcher.dispatch(saved, updated.pullDomainEvents());
    }
}
