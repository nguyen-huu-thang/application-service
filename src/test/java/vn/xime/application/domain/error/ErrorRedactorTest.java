package vn.xime.application.domain.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorRedactorTest {

    // ===== PUBLIC: passes through on every channel =====

    @Test
    void public_passesThroughOnRest() {
        assertThat(ErrorRedactor.forChannel(ErrorCode.APPLICATION_NOT_FOUND, Channel.REST_EXTERNAL))
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    void public_passesThroughOnGrpc() {
        assertThat(ErrorRedactor.forChannel(ErrorCode.APPLICATION_NOT_FOUND, Channel.GRPC_INTERNAL))
                .isEqualTo(ErrorCode.APPLICATION_NOT_FOUND);
    }

    // ===== SYSTEM: visible on gRPC internal, redacted on REST =====

    @Test
    void system_passesOnGrpcInternal() {
        assertThat(ErrorRedactor.forChannel(ErrorCode.SUBJECT_NOT_FOUND, Channel.GRPC_INTERNAL))
                .isEqualTo(ErrorCode.SUBJECT_NOT_FOUND);
    }

    @Test
    void system_redactedToGenericFamilyOnRest() {
        // SUBJECT_NOT_FOUND (NOT_FOUND family) -> common NOT_FOUND
        assertThat(ErrorRedactor.forChannel(ErrorCode.SUBJECT_NOT_FOUND, Channel.REST_EXTERNAL))
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    // ===== PRIVATE: redacted to UNKNOWN on every channel =====

    @Test
    void private_redactedToUnknownOnGrpc() {
        assertThat(ErrorRedactor.forChannel(ErrorCode.EVENT_SERIALIZATION_FAILED, Channel.GRPC_INTERNAL))
                .isEqualTo(ErrorCode.UNKNOWN);
    }

    @Test
    void private_redactedToUnknownOnRest() {
        assertThat(ErrorRedactor.forChannel(ErrorCode.OUTBOX_PUBLISH_FAILED, Channel.REST_EXTERNAL))
                .isEqualTo(ErrorCode.UNKNOWN);
    }
}
