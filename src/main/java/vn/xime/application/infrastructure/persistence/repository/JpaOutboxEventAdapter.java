package vn.xime.application.infrastructure.persistence.repository;

import java.time.Instant;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import vn.xime.application.application.dto.event.SubjectChangedEvent;
import vn.xime.application.application.port.out.event.SaveOutboxEventPort;
import vn.xime.application.common.exception.PrivateError;
import vn.xime.application.domain.error.ErrorCode;
import vn.xime.application.infrastructure.persistence.entity.OutboxEventEntity;

/**
 * Persists a SubjectChangedEvent into outbox_events in the current transaction.
 * Lưu SubjectChangedEvent vào outbox_events trong transaction hiện hành.
 *
 * Serialize event sang JSON; lỗi serialize ném PrivateError (nội bộ, không phơi ra ngoài).
 */
@Repository
@RequiredArgsConstructor
public class JpaOutboxEventAdapter implements SaveOutboxEventPort {

    private final OutboxEventJpaRepository outboxRepo;
    private final ObjectMapper objectMapper;

    @Override
    public void save(SubjectChangedEvent event) {
        String payload = serialize(event);

        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setTopic(SubjectChangedEvent.TOPIC);
        entity.setPayload(payload);
        entity.setCreatedAt(Instant.now());
        entity.setPublished(false);

        outboxRepo.save(entity);
    }

    private String serialize(SubjectChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException ex) {
            throw new PrivateError(ErrorCode.EVENT_SERIALIZATION_FAILED, ex.getMessage());
        }
    }
}
