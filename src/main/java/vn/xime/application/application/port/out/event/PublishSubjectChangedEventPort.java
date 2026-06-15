package vn.xime.application.application.port.out.event;

import vn.xime.application.application.dto.event.SubjectChangedEvent;

/**
 * Publishes a subject-changed event to the message broker (Kafka/NATS/...).
 * Phát sự kiện subject-changed lên message broker (Kafka/NATS/...).
 *
 * Gọi bởi outbox scheduler (infrastructure), không gọi trực tiếp trong use case.
 * Broker cụ thể nằm sau port này nên dễ swap.
 */
public interface PublishSubjectChangedEventPort {

    void publish(SubjectChangedEvent event);
}
