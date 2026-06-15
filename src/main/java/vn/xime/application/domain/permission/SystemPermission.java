package vn.xime.application.domain.permission;

import java.util.Objects;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Entity inside the Application aggregate: one granted system permission.
 * Entity nằm trong aggregate Application: một quyền hệ thống đã được cấp.
 *
 * Định danh theo (appId, permission) - một quyền chỉ cấp một lần cho một app.
 */
public final class SystemPermission {

    private final ApplicationId appId;
    private final PermissionCode permission;

    public SystemPermission(ApplicationId appId, PermissionCode permission) {
        this.appId = Objects.requireNonNull(appId, "appId is required");
        this.permission = Objects.requireNonNull(permission, "permission is required");
    }

    public ApplicationId appId() {
        return appId;
    }

    public PermissionCode permission() {
        return permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SystemPermission other)) {
            return false;
        }
        return appId.equals(other.appId) && permission == other.permission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, permission);
    }
}
