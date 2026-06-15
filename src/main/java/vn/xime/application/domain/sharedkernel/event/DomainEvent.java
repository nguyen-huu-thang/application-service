package vn.xime.application.domain.sharedkernel.event;

import java.time.Instant;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Marker interface for all domain events raised by aggregates.
 * Interface đánh dấu cho mọi domain event do aggregate raise.
 *
 * Domain event là pure Java object, không phụ thuộc framework, không phải Kafka message.
 * Use case pull ra sau khi save và dispatch để map sang sự kiện ngoài (SubjectChangedEvent).
 */
public interface DomainEvent {

    ApplicationId applicationId();

    Instant occurredAt();
}
