package vn.xime.application.application.usecase.application;

import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

import vn.xime.application.application.dto.application.RegisterApplicationCommand;
import vn.xime.application.application.dto.application.RegisterApplicationResult;
import vn.xime.application.application.port.in.application.RegisterApplicationUseCase;
import vn.xime.application.application.port.out.application.CheckApplicationCodeExistsPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;
import vn.xime.application.domain.application.ApplicationDescription;
import vn.xime.application.domain.application.ApplicationName;
import vn.xime.application.domain.application.exception.DuplicateApplicationCodeException;
import vn.xime.application.domain.sharedkernel.factory.ApplicationIdFactory;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Orchestrates registering a new application: build VO -> check uniqueness -> save.
 * Điều phối đăng ký application mới: dựng VO -> kiểm tra trùng -> lưu.
 */
@RequiredArgsConstructor
public class RegisterApplicationUseCaseImpl implements RegisterApplicationUseCase {

    private final CheckApplicationCodeExistsPort checkCodePort;
    private final SaveApplicationPort savePort;
    private final ApplicationDomainEventDispatcher eventDispatcher;

    @Override
    @Transactional
    public RegisterApplicationResult register(RegisterApplicationCommand command) {

        ApplicationCode code = ApplicationCode.of(command.applicationCode());

        if (checkCodePort.existsByCode(code)) {
            throw new DuplicateApplicationCodeException(code.value());
        }

        ApplicationName name = ApplicationName.of(command.name());
        ApplicationDescription description = ApplicationDescription.of(command.description());
        ApplicationId id = ApplicationIdFactory.generate();

        // tenantId null trong giai đoạn single-tenant.
        Application app = Application.register(id, code, name, description, null);

        Application saved = savePort.save(app);
        eventDispatcher.dispatch(saved, app.pullDomainEvents());

        return new RegisterApplicationResult(saved.getId(), saved.getCode().value());
    }
}
