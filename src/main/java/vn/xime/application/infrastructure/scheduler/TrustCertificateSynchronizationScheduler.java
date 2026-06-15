package vn.xime.application.infrastructure.scheduler;

import jakarta.annotation.PostConstruct;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import vn.xime.application.integration.trust.cert.TrustCertificateSynchronizer;
import vn.xime.application.integration.trust.publicca.TrustRootCertificateInitializer;

/**
 * Drives trust certificate synchronization at startup and periodically.
 * Điều phối đồng bộ certificate trust lúc startup và định kỳ.
 *
 * @PostConstruct: nạp root CA rồi đồng bộ cert (bootstrap/rotate) NGAY khi context init -
 * gRPC server (mTLS) @DependsOn bean này nên cert đã sẵn sàng trước khi server start.
 * @Scheduled 24h: rotate runtime cert (cần @EnableScheduling - xem SchedulingConfig).
 */
@Component
@RequiredArgsConstructor
public class TrustCertificateSynchronizationScheduler {

    private final TrustRootCertificateInitializer trustRootCertificateInitializer;
    private final TrustCertificateSynchronizer trustCertificateSynchronizer;

    @PostConstruct
    public void startup() {
        try {
            trustRootCertificateInitializer.initialize();
            trustCertificateSynchronizer.synchronizeOnStartup();
        } catch (Exception exception) {
            // fatal startup failure: không thiết lập được trust thì dừng
            throw exception;
        }
    }

    @Scheduled(initialDelay = 24 * 60 * 60 * 1000L, fixedDelay = 24 * 60 * 60 * 1000L)
    public void synchronize() {
        try {
            trustCertificateSynchronizer.synchronize();
        } catch (Exception exception) {
            // nuốt lỗi sync định kỳ: runtime vẫn dùng cert hiện tại còn hợp lệ
        }
    }
}
