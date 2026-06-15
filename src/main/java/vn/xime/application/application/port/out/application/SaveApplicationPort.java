package vn.xime.application.application.port.out.application;

import vn.xime.application.domain.application.Application;

/**
 * Persists an application aggregate.
 * Lưu aggregate application.
 *
 * Contract: khi status hoặc permission thay đổi, adapter cấp change_sequence mới
 * (monotonic) và trả về aggregate đã lưu mang change_sequence hiện hành - dùng để
 * dựng snapshot SubjectChangedEvent cho outbox.
 */
public interface SaveApplicationPort {

    Application save(Application application);
}
