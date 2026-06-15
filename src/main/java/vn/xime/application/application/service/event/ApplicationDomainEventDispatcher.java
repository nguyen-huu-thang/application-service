package vn.xime.application.application.service.event;

import java.util.List;

import lombok.RequiredArgsConstructor;

import vn.xime.application.application.dto.event.SubjectChangedEvent;
import vn.xime.application.application.dto.event.SubjectChangedEventType;
import vn.xime.application.application.port.out.event.SaveOutboxEventPort;
import vn.xime.application.domain.application.event.ApplicationRegisteredEvent;
import vn.xime.application.domain.permission.event.SystemPermissionGrantedEvent;
import vn.xime.application.domain.permission.event.SystemPermissionRevokedEvent;
import vn.xime.application.domain.sharedkernel.event.DomainEvent;

/**
 * Maps domain events raised by an aggregate into one outbox snapshot for resource services.
 * Map các domain event do aggregate raise thành một snapshot outbox cho resource service.
 *
 * Điều phối thuần application (không chứa business rule). Khác design gốc: nhận thẳng
 * aggregate đã lưu (có change_sequence mới) thay vì reload theo từng event, và gộp nhiều
 * domain event của một thao tác thành đúng một SubjectChangedEvent (snapshot) - tránh
 * double event và truy vấn thừa.
 */
@RequiredArgsConstructor
public class ApplicationDomainEventDispatcher {

    private final SaveOutboxEventPort outboxPort;

    /**
     * Dispatches the events of one operation: writes a snapshot to outbox if any event
     * is sync-worthy. ApplicationRegisteredEvent một mình không sync (app còn PENDING_REVIEW).
     */
    public void dispatch(vn.xime.application.domain.application.Application savedSnapshot,
                         List<DomainEvent> events) {

        if (events.isEmpty()) {
            return;
        }

        boolean syncWorthy = events.stream().anyMatch(ApplicationDomainEventDispatcher::requiresSync);

        if (!syncWorthy) {
            return;
        }

        SubjectChangedEventType type = resolveType(events);
        outboxPort.save(SubjectChangedEvent.from(savedSnapshot, type));
    }

    private static boolean requiresSync(DomainEvent event) {
        // Đăng ký mới chưa active -> resource service chưa cần biết.
        return !(event instanceof ApplicationRegisteredEvent);
    }

    private static SubjectChangedEventType resolveType(List<DomainEvent> events) {
        boolean permissionChange = events.stream().anyMatch(e ->
                e instanceof SystemPermissionGrantedEvent
                        || e instanceof SystemPermissionRevokedEvent);

        return permissionChange
                ? SubjectChangedEventType.APPLICATION_PERMISSION_CHANGED
                : SubjectChangedEventType.APPLICATION_STATUS_CHANGED;
    }
}
