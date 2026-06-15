package vn.xime.application.config.scheduling;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled tasks (trust certificate rotation runs every 24h).
 * Bật các task @Scheduled (rotate certificate trust chạy mỗi 24h).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
