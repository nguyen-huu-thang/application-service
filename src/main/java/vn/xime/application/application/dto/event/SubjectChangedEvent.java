package vn.xime.application.application.dto.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.permission.SystemPermission;

/**
 * External notification carrying a full snapshot of an application's subject state.
 * Thông báo ngoài mang ảnh chụp đầy đủ trạng thái subject của một application.
 *
 * Khác domain event (fine-grained, nội bộ): đây là snapshot toàn bộ state - consumer
 * nhận là cập nhật cache ngay, không cần gọi thêm API. Được ghi vào outbox và publish
 * lên topic "application.subject.changed".
 */
public record SubjectChangedEvent(
        String eventId,
        SubjectChangedEventType eventType,
        String identityId,      // hex 24 byte
        String status,
        List<String> permissions,
        long stateVersion,
        long changeSequence,
        String tenantId,        // nullable
        Instant occurredAt
) {

    public static final String TOPIC = "application.subject.changed";

    /**
     * Builds a snapshot from the persisted aggregate (with DB-assigned change_sequence).
     * Dựng snapshot từ aggregate đã lưu (đã có change_sequence do DB gán).
     */
    public static SubjectChangedEvent from(Application app, SubjectChangedEventType type) {
        List<String> perms = app.getPermissions().stream()
                .map(SystemPermission::permission)
                .map(Enum::name)
                .toList();

        String tenant = app.getTenantId() == null ? null : app.getTenantId().value();

        return new SubjectChangedEvent(
                UUID.randomUUID().toString(),
                type,
                app.getId().toHex(),
                app.getStatus().name(),
                perms,
                app.getStateVersion(),
                app.getChangeSequence(),
                tenant,
                Instant.now()
        );
    }
}
