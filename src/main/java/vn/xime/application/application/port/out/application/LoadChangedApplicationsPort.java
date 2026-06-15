package vn.xime.application.application.port.out.application;

import java.util.List;

import vn.xime.application.domain.application.Application;

/**
 * Loads applications whose change_sequence is greater than a cursor, ascending.
 * Tải các application có change_sequence lớn hơn con trỏ, theo thứ tự tăng dần.
 *
 * Dùng cho pull-based sync (PollChangedApplications).
 */
public interface LoadChangedApplicationsPort {

    List<Application> findByChangeSequenceAfter(long afterSequence, int limit);
}
