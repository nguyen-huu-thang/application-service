package vn.xime.application.domain.sharedkernel.model;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;

/**
 * Identity id of an APPLICATION subject: a 24-byte KSUID.
 * Định danh của subject loại APPLICATION: KSUID 24 byte.
 *
 * Bất biến, so sánh theo nội dung byte. Khác trust-service (20 byte),
 * mọi service ngoài Trust dùng 24 byte.
 */
public final class ApplicationId {

    public static final int LENGTH = 24;

    private static final int TIMESTAMP_LENGTH = 4;

    private static final long KSUID_EPOCH = 1400000000L;

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final byte[] value;

    public ApplicationId(byte[] value) {

        if (value == null) {
            throw new IllegalArgumentException("ApplicationId cannot be null");
        }

        if (value.length != LENGTH) {
            throw new IllegalArgumentException(
                    "ApplicationId must be " + LENGTH + " bytes, got " + value.length
            );
        }

        this.value = Arrays.copyOf(value, value.length);
    }

    // =========================
    // CORE BEHAVIOR
    // =========================

    public byte[] toBytes() {
        return Arrays.copyOf(value, value.length);
    }

    public String toHex() {
        char[] hex = new char[value.length * 2];

        for (int i = 0; i < value.length; i++) {
            int v = value[i] & 0xFF;
            hex[i * 2] = HEX[v >>> 4];
            hex[i * 2 + 1] = HEX[v & 0x0F];
        }

        return new String(hex);
    }

    public Instant getTimestamp() {

        int ts = ByteBuffer
                .wrap(value, 0, TIMESTAMP_LENGTH)
                .getInt();

        long epoch = (ts & 0xFFFFFFFFL) + KSUID_EPOCH;

        return Instant.ofEpochSecond(epoch);
    }

    // =========================
    // EQUALITY
    // =========================

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof ApplicationId other)) {
            return false;
        }

        return Arrays.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(value);
    }

    @Override
    public String toString() {
        return toHex();
    }
}
