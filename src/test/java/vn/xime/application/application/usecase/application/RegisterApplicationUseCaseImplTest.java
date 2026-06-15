package vn.xime.application.application.usecase.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.xime.application.application.dto.application.RegisterApplicationCommand;
import vn.xime.application.application.dto.application.RegisterApplicationResult;
import vn.xime.application.application.port.out.application.CheckApplicationCodeExistsPort;
import vn.xime.application.application.port.out.application.SaveApplicationPort;
import vn.xime.application.application.service.event.ApplicationDomainEventDispatcher;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.exception.DuplicateApplicationCodeException;

@ExtendWith(MockitoExtension.class)
class RegisterApplicationUseCaseImplTest {

    @Mock
    CheckApplicationCodeExistsPort checkCodePort;
    @Mock
    SaveApplicationPort savePort;
    @Mock
    ApplicationDomainEventDispatcher eventDispatcher;

    @InjectMocks
    RegisterApplicationUseCaseImpl useCase;

    @Test
    void register_success_savesAndReturnsResult_andDispatches() {
        when(checkCodePort.existsByCode(any())).thenReturn(false);
        when(savePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RegisterApplicationResult result = useCase.register(
                new RegisterApplicationCommand("Xime-Social", "Xime Social", "desc"));

        assertThat(result.applicationCode()).isEqualTo("xime-social");
        assertThat(result.identityId()).isNotNull();
        verify(savePort).save(any(Application.class));
        verify(eventDispatcher).dispatch(any(Application.class), any());
    }

    @Test
    void register_duplicateCode_throws_andDoesNotSave() {
        when(checkCodePort.existsByCode(any())).thenReturn(true);

        assertThatThrownBy(() -> useCase.register(
                new RegisterApplicationCommand("xime-social", "Xime Social", null)))
                .isInstanceOf(DuplicateApplicationCodeException.class);

        verify(savePort, never()).save(any());
        verify(eventDispatcher, never()).dispatch(any(), any());
    }

    @Test
    void register_invalidCode_throwsValidation_andDoesNotCheckOrSave() {
        assertThatThrownBy(() -> useCase.register(
                new RegisterApplicationCommand("Bad_Code!", "name", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(checkCodePort, never()).existsByCode(any());
        verify(savePort, never()).save(any());
    }
}
