package vn.xime.application.application.usecase.permission;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.permission.RevokeSystemPermissionCommand;
import vn.xime.application.application.port.in.permission.RevokeSystemPermissionUseCase;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;
import vn.xime.application.domain.permission.PermissionCode;

/**
 * Orchestrates revoking a permission: parse code -> load -> domain.revoke() -> save -> dispatch.
 * Điều phối thu hồi quyền: phân giải code -> load -> domain.revoke() -> save -> phát sự kiện.
 */
@RequiredArgsConstructor
public class RevokeSystemPermissionUseCaseImpl implements RevokeSystemPermissionUseCase {

    private final LoadApplicationPort loadPort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public void revoke(RevokeSystemPermissionCommand command) {

        PermissionCode permission = PermissionCode.from(command.permission());

        Application app = loadPort.findById(command.appId())
                .orElseThrow(() -> new ApplicationNotFoundException(command.appId().toHex()));

        Application updated = app.revokePermission(permission);

        Application saved = savePort.save(updated);
        eventDispatcher.dispatch(saved, updated.pullDomainEvents());
    }
}
