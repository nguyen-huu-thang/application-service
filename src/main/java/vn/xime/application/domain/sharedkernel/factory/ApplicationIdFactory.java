package vn.xime.application.domain.sharedkernel.factory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Instant;

import vn.xime.application.domain.sharedkernel.model.ApplicationId;

/**
 * Generates a 24-byte KSUID ApplicationId: 4-byte timestamp + 20-byte randomness.
 * Sinh ApplicationId KSUID 24 byte: 4 byte timestamp + 20 byte ngẫu nhiên.
 *
 * Kế thừa pattern IdFactory của user-service.
 */
public final class ApplicationIdFactory {

    private static final int TIMESTAMP_LENGTH = 4;
    private static final int RANDOM_LENGTH = 20;

    private static final long KSUID_EPOCH = 1400000000L;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ApplicationIdFactory() {
    }

    public static ApplicationId generate() {
        byte[] bytes = new byte[ApplicationId.LENGTH];

        // timestamp (4 bytes)
        long now = Instant.now().getEpochSecond() - KSUID_EPOCH;
        ByteBuffer.wrap(bytes, 0, TIMESTAMP_LENGTH).putInt((int) now);

        // random (20 bytes)
        byte[] random = new byte[RANDOM_LENGTH];
        RANDOM.nextBytes(random);
        System.arraycopy(random, 0, bytes, TIMESTAMP_LENGTH, RANDOM_LENGTH);

        return new ApplicationId(bytes);
    }
}
