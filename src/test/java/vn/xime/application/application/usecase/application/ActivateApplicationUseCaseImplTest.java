package vn.xime.application.application.usecase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.xime.application.application.dto.application.ActivateApplicationCommand;
import vn.xime.application.application.port.out.application.LoadApplicationPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;
import vn.xime.application.domain.application.ApplicationDescription;
import vn.xime.application.domain.application.ApplicationName;
import vn.xime.application.domain.application.ApplicationStatus;
import vn.xime.application.domain.application.exception.ApplicationNotFoundException;
import vn.xime.application.domain.sharedkernel.factory.ApplicationIdFactory;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

@ExtendWith(MockitoExtension.class)
class ActivateApplicationUseCaseImplTest {

    @Mock
    LoadApplicationPort loadPort;
    @Mock
    SaveApplicationPort savePort;
    @Mock
    ApplicationDomainEventDispatcher eventDispatcher;

    @InjectMocks
    ActivateApplicationUseCaseImpl useCase;

    private Application pending() {
        return Application.register(
                ApplicationIdFactory.generate(),
                ApplicationCode.of("xime-social"),
                ApplicationName.of("Xime Social"),
                ApplicationDescription.of(null),
                null);
    }

    @Test
    void activate_success_savesActiveApp_andDispatches() {
        Application pending = pending();
        when(loadPort.findById(any())).thenReturn(Optional.of(pending));
        when(savePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.activate(new ActivateApplicationCommand(pending.getId()));

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(savePort).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ApplicationStatus.ACTIVE);
        verify(eventDispatcher).dispatch(any(Application.class), any());
    }

    @Test
    void activate_notFound_throws_andDoesNotSave() {
        ApplicationId id = ApplicationIdFactory.generate();
        when(loadPort.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.activate(new ActivateApplicationCommand(id)))
                .isInstanceOf(ApplicationNotFoundException.class);

        verify(savePort, never()).save(any());
        verify(eventDispatcher, never()).dispatch(any(), any());
    }
}
