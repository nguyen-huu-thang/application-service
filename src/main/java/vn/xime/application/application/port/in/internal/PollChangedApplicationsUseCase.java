package vn.xime.application.application.port.in.internal;

import vn.xime.application.application.dto.internal.ChangedApplicationsResult;
import vn.xime.application.application.dto.internal.PollChangedApplicationsQuery;

/**
 * Pulls applications changed after a change_sequence cursor for sync reconcile.
 * Kéo các application thay đổi sau con trỏ change_sequence cho đồng bộ reconcile.
 */
public interface PollChangedApplicationsUseCase {

    ChangedApplicationsResult poll(PollChangedApplicationsQuery query);
}
