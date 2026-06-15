package vn.xime.application.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.xime.application.infrastructure.persistence.entity.OutboxEventEntity;

/**
 * Spring Data repository for OutboxEventEntity.
 * Repository Spring Data cho OutboxEventEntity.
 *
 * Scheduler publish (thêm khi chọn broker) sẽ bổ sung finder cho bản ghi published=false.
 */
public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
}
