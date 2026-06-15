package vn.xime.application.infrastructure.security.bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the bootstrap file (base64 -> JSON -> payload).
 * Đọc file bootstrap (base64 -> JSON -> payload).
 *
 * Spring Boot 4 dùng Jackson 3 (tools.jackson); readValue ném JacksonException (unchecked).
 */
public class BootstrapLoader {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    public BootstrapPayload load(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }

        try {
            String encoded = Files.readString(path, StandardCharsets.UTF_8);
            byte[] decoded = Base64.getDecoder().decode(encoded.trim());
            String json = new String(decoded, StandardCharsets.UTF_8);
            return OBJECT_MAPPER.readValue(json, BootstrapPayload.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load bootstrap file: " + path, e);
        }
    }
}
