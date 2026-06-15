package vn.xime.application.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import vn.xime.application.domain.application.event.ApplicationActivatedEvent;
import vn.xime.application.domain.application.event.ApplicationRegisteredEvent;
import vn.xime.application.domain.application.exception.InvalidStatusTransitionException;
import vn.xime.application.domain.permission.PermissionCode;
import vn.xime.application.domain.permission.event.SystemPermissionGrantedEvent;
import vn.xime.application.domain.permission.event.SystemPermissionRevokedEvent;
import vn.xime.application.domain.permission.exception.PermissionAlreadyGrantedException;
import vn.xime.application.domain.permission.exception.PermissionNotGrantedException;
import vn.xime.application.domain.sharedkernel.event.DomainEvent;
import vn.xime.application.domain.sharedkernel.factory.ApplicationIdFactory;
import vn.xime.application.domain.sharedkernel.model.ApplicationId;

class ApplicationTest {

    private Application newPending() {
        ApplicationId id = ApplicationIdFactory.generate();
        return Application.register(
                id,
                ApplicationCode.of("xime-social"),
                ApplicationName.of("Xime Social"),
                ApplicationDescription.of("desc"),
                null);
    }

    private Application active() {
        return newPending().activate();
    }

    // =========================
    // REGISTER
    // =========================

    @Test
    void register_startsInPendingReview_versionZero_raisesRegisteredEvent() {
        Application app = newPending();

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        assertThat(app.getStateVersion()).isZero();
        assertThat(app.getPermissions()).isEmpty();

        List<DomainEvent> events = app.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(ApplicationRegisteredEvent.class);
    }

    @Test
    void pullDomainEvents_clearsAfterPull() {
        Application app = newPending();

        assertThat(app.pullDomainEvents()).hasSize(1);
        assertThat(app.pullDomainEvents()).isEmpty();
    }

    // =========================
    // STATE TRANSITIONS - HAPPY PATH
    // =========================

    @Test
    void activate_pendingToActive_incrementsVersion_raisesActivatedEvent() {
        Application activated = newPending().activate();

        assertThat(activated.getStatus()).isEqualTo(ApplicationStatus.ACTIVE);
        assertThat(activated.getStateVersion()).isEqualTo(1);
        assertThat(activated.pullDomainEvents().get(0))
                .isInstanceOf(ApplicationActivatedEvent.class);
    }

    @Test
    void fullLifecycle_activate_suspend_reactivate_disable_retire() {
        Application app = newPending().activate();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACTIVE);

        app = app.suspend();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SUSPENDED);

        app = app.reactivate();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.ACTIVE);

        app = app.disable();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DISABLED);

        app = app.retire();
        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.RETIRED);
        assertThat(app.getStateVersion()).isEqualTo(5);
    }

    @Test
    void disable_allowedFromSuspended() {
        Application disabled = newPending().activate().suspend().disable();
        assertThat(disabled.getStatus()).isEqualTo(ApplicationStatus.DISABLED);
    }

    // =========================
    // STATE TRANSITIONS - GUARDS
    // =========================

    @Test
    void activate_fromActive_throws() {
        Application active = active();
        assertThatThrownBy(active::activate)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void suspend_fromPending_throws() {
        Application pending = newPending();
        assertThatThrownBy(pending::suspend)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void reactivate_fromActive_throws() {
        Application active = active();
        assertThatThrownBy(active::reactivate)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void retire_fromActive_throws() {
        Application active = active();
        assertThatThrownBy(active::retire)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void disable_fromPending_throws() {
        Application pending = newPending();
        assertThatThrownBy(pending::disable)
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    // =========================
    // IMMUTABILITY
    // =========================

    @Test
    void transition_doesNotMutateOriginal() {
        Application pending = newPending();
        Application activated = pending.activate();

        assertThat(pending.getStatus()).isEqualTo(ApplicationStatus.PENDING_REVIEW);
        assertThat(pending.getStateVersion()).isZero();
        assertThat(activated).isNotSameAs(pending);
    }

    @Test
    void getPermissions_isUnmodifiable() {
        Application app = active().grantPermission(PermissionCode.DATA_READ_OBJECT);
        assertThatThrownBy(() -> app.getPermissions().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // =========================
    // PERMISSIONS
    // =========================

    @Test
    void grantPermission_addsPermission_raisesEvent_incrementsVersion() {
        Application active = active();
        long versionBefore = active.getStateVersion();

        Application granted = active.grantPermission(PermissionCode.DATA_READ_OBJECT);

        assertThat(granted.hasPermission(PermissionCode.DATA_READ_OBJECT)).isTrue();
        assertThat(granted.getPermissions()).hasSize(1);
        assertThat(granted.getStateVersion()).isEqualTo(versionBefore + 1);
        assertThat(granted.pullDomainEvents().get(0))
                .isInstanceOf(SystemPermissionGrantedEvent.class);
    }

    @Test
    void grantPermission_duplicate_throws() {
        Application granted = active().grantPermission(PermissionCode.DATA_READ_OBJECT);
        assertThatThrownBy(() -> granted.grantPermission(PermissionCode.DATA_READ_OBJECT))
                .isInstanceOf(PermissionAlreadyGrantedException.class);
    }

    @Test
    void revokePermission_removesPermission_raisesEvent() {
        Application granted = active().grantPermission(PermissionCode.DATA_READ_OBJECT);

        Application revoked = granted.revokePermission(PermissionCode.DATA_READ_OBJECT);

        assertThat(revoked.hasPermission(PermissionCode.DATA_READ_OBJECT)).isFalse();
        assertThat(revoked.getPermissions()).isEmpty();
        assertThat(revoked.pullDomainEvents().get(0))
                .isInstanceOf(SystemPermissionRevokedEvent.class);
    }

    @Test
    void revokePermission_notGranted_throws() {
        Application active = active();
        assertThatThrownBy(() -> active.revokePermission(PermissionCode.DATA_READ_OBJECT))
                .isInstanceOf(PermissionNotGrantedException.class);
    }
}
