package vn.xime.application.application.service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.xime.application.application.dto.event.SubjectChangedEvent;
import vn.xime.application.application.dto.event.SubjectChangedEventType;
import vn.xime.application.application.port.out.event.SaveOutboxEventPort;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.application.ApplicationCode;
import vn.xime.application.domain.application.ApplicationDescription;
import vn.xime.application.domain.application.ApplicationName;
import vn.xime.application.domain.application.ApplicationStatus;
import vn.xime.application.domain.application.event.ApplicationActivatedEvent;
import vn.xime.application.domain.application.event.ApplicationRegisteredEvent;
import vn.xime.application.domain.permission.PermissionCode;
import vn.xime.application.domain.permission.event.SystemPermissionGrantedEvent;
import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.factory.ApplicationIdFactory;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

@ExtendWith(MockitoExtension.class)
class ApplicationDomainEventDispatcherTest {

    @Mock
    SaveOutboxEventPort outboxPort;

    private final ApplicationId id = ApplicationIdFactory.generate();

    private ApplicationDomainEventDispatcher dispatcher() {
        return new ApplicationDomainEventDispatcher(outboxPort);
    }

    private Application snapshot() {
        Instant now = Instant.now();
        return Application.reconstitute(
                id,
                ApplicationCode.of("xime-social"),
                ApplicationName.of("Xime Social"),
                ApplicationDescription.of(null),
                ApplicationStatus.ACTIVE,
                3L, 42L, null, List.of(), now, now);
    }

    @Test
    void emptyEvents_noOutbox() {
        dispatcher().dispatch(snapshot(), List.of());
        verify(outboxPort, never()).save(any());
    }

    @Test
    void registeredEventOnly_noOutbox() {
        List<DomainEvent> events = List.of(
                new ApplicationRegisteredEvent(id, "xime-social", "Xime Social", Instant.now()));
        dispatcher().dispatch(snapshot(), events);
        verify(outboxPort, never()).save(any());
    }

    @Test
    void activatedEvent_savesStatusChangedSnapshot() {
        List<DomainEvent> events = List.of(new ApplicationActivatedEvent(id, Instant.now()));

        dispatcher().dispatch(snapshot(), events);

        ArgumentCaptor<SubjectChangedEvent> captor = ArgumentCaptor.forClass(SubjectChangedEvent.class);
        verify(outboxPort).save(captor.capture());
        SubjectChangedEvent event = captor.getValue();
        assertThat(event.eventType()).isEqualTo(SubjectChangedEventType.APPLICATION_STATUS_CHANGED);
        assertThat(event.changeSequence()).isEqualTo(42L);
        assertThat(event.identityId()).isEqualTo(id.toHex());
    }

    @Test
    void permissionEvent_savesPermissionChangedSnapshot() {
        List<DomainEvent> events = List.of(
                new SystemPermissionGrantedEvent(id, PermissionCode.DATA_READ_OBJECT, Instant.now()));

        dispatcher().dispatch(snapshot(), events);

        ArgumentCaptor<SubjectChangedEvent> captor = ArgumentCaptor.forClass(SubjectChangedEvent.class);
        verify(outboxPort).save(captor.capture());
        assertThat(captor.getValue().eventType())
                .isEqualTo(SubjectChangedEventType.APPLICATION_PERMISSION_CHANGED);
    }
}
