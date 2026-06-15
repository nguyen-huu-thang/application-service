package vn.xime.application.application.dto.application;

import java.time.Instant;
import java.util.List;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Full read model of an application returned to adapters.
 * Mô hình đọc đầy đủ của một application trả về cho adapter.
 *
 * Adapter (gRPC/REST) chịu trách nhiệm chuyển sang định dạng dây (bytes, epoch millis).
 */
public record ApplicationResult(
        ApplicationId identityId,
        String applicationCode,
        String name,
        String description,
        String status,
        long stateVersion,
        long changeSequence,
        List<String> permissions,
        String tenantId,
        Instant createdAt,
        Instant updatedAt
) {
}
