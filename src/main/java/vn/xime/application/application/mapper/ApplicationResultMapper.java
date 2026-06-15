package vn.xime.application.application.mapper;

import java.util.List;

import vn.xime.application.application.dto.application.ApplicationResult;
import vn.xime.application.application.dto.application.ApplicationSummaryResult;
import vn.xime.application.application.dto.internal.SubjectInfoResult;
import vn.xime.application.domain.application.Application;
import vn.xime.application.domain.permission.SystemPermission;
import vn.xime.application.domain.sharedkernel.model.TenantId;

/**
 * Maps Application aggregate to read-model DTOs, unwrapping value objects.
 * Map aggregate Application sang DTO đọc, mở bọc các value object.
 *
 * Static thuần - không giữ state, không cần wire bean.
 */
public final class ApplicationResultMapper {

    public static final String SUBJECT_TYPE_APPLICATION = "APPLICATION";

    private ApplicationResultMapper() {
    }

    public static ApplicationResult toResult(Application app) {
        return new ApplicationResult(
                app.getId(),
                app.getCode().value(),
                app.getName().value(),
                app.getDescription().value(),
                app.getStatus().name(),
                app.getStateVersion(),
                app.getChangeSequence(),
                permissionNames(app),
                tenantIdValue(app.getTenantId()),
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    public static ApplicationSummaryResult toSummary(Application app) {
        return new ApplicationSummaryResult(
                app.getId(),
                app.getCode().value(),
                app.getName().value(),
                app.getStatus().name()
        );
    }

    public static SubjectInfoResult toSubjectInfo(Application app) {
        return new SubjectInfoResult(
                app.getId(),
                SUBJECT_TYPE_APPLICATION,
                app.getStatus().name(),
                app.getStateVersion(),
                app.getChangeSequence(),
                permissionNames(app),
                tenantIdValue(app.getTenantId())
        );
    }

    private static List<String> permissionNames(Application app) {
        return app.getPermissions().stream()
                .map(SystemPermission::permission)
                .map(Enum::name)
                .toList();
    }

    private static String tenantIdValue(TenantId tenantId) {
        return tenantId == null ? null : tenantId.value();
    }
}
