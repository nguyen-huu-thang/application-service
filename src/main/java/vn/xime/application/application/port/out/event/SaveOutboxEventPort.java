package vn.xime.application.application.port.out.event;

import vn.xime.application.application.dto.event.SubjectChangedEvent;

/**
 * Saves a subject-changed event into the transactional outbox.
 * Lưu sự kiện subject-changed vào outbox trong cùng transaction.
 *
 * Ghi cùng transaction với save aggregate để tránh dual-write / lost event.
 * Một scheduler (infrastructure) đọc outbox và publish lên broker.
 */
public interface SaveOutboxEventPort {

    void save(SubjectChangedEvent event);
}
