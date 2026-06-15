package vn.xime.application.config.usecase;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vn.xime.application.application.port.in.application.ActivateApplicationUseCase;
import vn.xime.application.application.port.in.application.DisableApplicationUseCase;
import vn.xime.application.application.port.in.application.GetApplicationUseCase;
import vn.xime.application.application.port.in.application.ListApplicationsUseCase;
import vn.xime.application.application.port.in.application.ReactivateApplicationUseCase;
import vn.xime.application.application.port.in.application.RegisterApplicationUseCase;
import vn.xime.application.application.port.in.application.RetireApplicationUseCase;
import vn.xime.application.application.port.in.application.SuspendApplicationUseCase;
import vn.xime.application.application.port.in.internal.GetSubjectInfoUseCase;
import vn.xime.application.application.port.in.internal.PollChangedApplicationsUseCase;
import vn.xime.application.application.port.in.permission.GrantSystemPermissionUseCase;
import vn.xime.application.application.port.in.permission.RevokeSystemPermissionUseCase;
import vn.xime.application.application.port.out.application.CheckApplicationCodeExistsPort;
import vn.xime.application.application.port.out.application.ListApplicationsPort;
import vn.xime.application.application.port.out.application.LoadApplicationByCodePort;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.LoadChangedApplicationsPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.port.out.event.SaveOutboxEventPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.application.usecase.application.ActivateApplicationUseCaseImpl;
import vn.xime.application.application.usecase.application.DisableApplicationUseCaseImpl;
import vn.xime.application.application.usecase.application.GetApplicationUseCaseImpl;
import vn.xime.application.application.usecase.application.ListApplicationsUseCaseImpl;
import vn.xime.application.application.usecase.application.ReactivateApplicationUseCaseImpl;
import vn.xime.application.application.usecase.application.RegisterApplicationUseCaseImpl;
import vn.xime.application.application.usecase.application.RetireApplicationUseCaseImpl;
import vn.xime.application.application.usecase.application.SuspendApplicationUseCaseImpl;
import vn.xime.application.application.usecase.internal.GetSubjectInfoUseCaseImpl;
import vn.xime.application.application.usecase.internal.PollChangedApplicationsUseCaseImpl;
import vn.xime.application.application.usecase.permission.GrantSystemPermissionUseCaseImpl;
import vn.xime.application.application.usecase.permission.RevokeSystemPermissionUseCaseImpl;

/**
 * Wires use case beans and the domain event dispatcher (impls are plain classes).
 * Wire các bean use case và domain event dispatcher (impl là class thuần).
 *
 * Theo architecture-rules: use case không gắn @Component, đăng ký bean tập trung tại đây.
 * Out-port impl là bean @Repository ở tầng infrastructure, Spring inject theo type.
 */
@Configuration
public class UseCaseConfig {

    // =========================
    // EVENT DISPATCHER
    // =========================

    @Bean
    public ApplicationDomainEventDispatcher applicationDomainEventDispatcher(
            SaveOutboxEventPort saveOutboxEventPort) {
        return new ApplicationDomainEventDispatcher(saveOutboxEventPort);
    }

    // =========================
    // APPLICATION LIFECYCLE
    // =========================

    @Bean
    public RegisterApplicationUseCase registerApplicationUseCase(
            CheckApplicationCodeExistsPort checkCodePort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new RegisterApplicationUseCaseImpl(checkCodePort, savePort, eventDispatcher);
    }

    @Bean
    public ActivateApplicationUseCase activateApplicationUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new ActivateApplicationUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    @Bean
    public SuspendApplicationUseCase suspendApplicationUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new SuspendApplicationUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    @Bean
    public ReactivateApplicationUseCase reactivateApplicationUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new ReactivateApplicationUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    @Bean
    public DisableApplicationUseCase disableApplicationUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new DisableApplicationUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    @Bean
    public RetireApplicationUseCase retireApplicationUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new RetireApplicationUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    // =========================
    // APPLICATION QUERY
    // =========================

    @Bean
    public GetApplicationUseCase getApplicationUseCase(
            LoadApplicationPort loadPort,
            LoadApplicationByCodePort loadByCodePort) {
        return new GetApplicationUseCaseImpl(loadPort, loadByCodePort);
    }

    @Bean
    public ListApplicationsUseCase listApplicationsUseCase(
            ListApplicationsPort listPort) {
        return new ListApplicationsUseCaseImpl(listPort);
    }

    // =========================
    // PERMISSION
    // =========================

    @Bean
    public GrantSystemPermissionUseCase grantSystemPermissionUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new GrantSystemPermissionUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    @Bean
    public RevokeSystemPermissionUseCase revokeSystemPermissionUseCase(
            LoadApplicationPort loadPort,
            SaveApplicationPort savePort,
            ApplicationDomainEventDispatcher eventDispatcher) {
        return new RevokeSystemPermissionUseCaseImpl(loadPort, savePort, eventDispatcher);
    }

    // =========================
    // INTERNAL (sync)
    // =========================

    @Bean
    public GetSubjectInfoUseCase getSubjectInfoUseCase(
            LoadApplicationPort loadPort) {
        return new GetSubjectInfoUseCaseImpl(loadPort);
    }

    @Bean
    public PollChangedApplicationsUseCase pollChangedApplicationsUseCase(
            LoadChangedApplicationsPort loadChangedPort) {
        return new PollChangedApplicationsUseCaseImpl(loadChangedPort);
    }
}
