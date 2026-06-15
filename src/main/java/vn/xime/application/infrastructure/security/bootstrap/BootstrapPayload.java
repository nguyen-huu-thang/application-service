package vn.xime.application.infrastructure.security.bootstrap;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base64-decoded JSON payload of the bootstrap file (first startup only).
 * Payload JSON (đã base64-decode) của file bootstrap (chỉ dùng lần khởi động đầu).
 *
 * Lưu ý: jackson-annotations vẫn ở package com.fasterxml.jackson.annotation kể cả với Jackson 3.
 */
public record BootstrapPayload(

        Certificate certificate,

        @JsonProperty("token_id")
        String tokenId,

        @JsonProperty("refresh_token")
        String refreshToken
) {

    public record Certificate(

            String id,

            @JsonProperty("service_id")
            String serviceId,

            @JsonProperty("public_cert")
            String publicCert,

            @JsonProperty("private_key")
            String privateKey,

            String status,

            @JsonProperty("issued_at")
            String issuedAt,

            @JsonProperty("expires_at")
            String expiresAt,

            boolean deleted
    ) {

        public Instant issuedAtInstant() {
            return Instant.ofEpochMilli(Long.parseLong(issuedAt));
        }

        public Instant expiresAtInstant() {
            return Instant.ofEpochMilli(Long.parseLong(expiresAt));
        }
    }
}
