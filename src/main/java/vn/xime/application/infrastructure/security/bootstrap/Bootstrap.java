package vn.xime.application.infrastructure.security.bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import vn.xime.application.integration.trust.model.Certificate;

/**
 * Bootstrap file access: existence, load+validate, map to runtime Certificate, delete.
 * Truy cập file bootstrap: kiểm tra tồn tại, load+validate, map sang Certificate runtime, xóa.
 *
 * Chỉ dùng cho lần khởi động đầu / thiết lập trust ban đầu, KHÔNG phải nơi lưu trust lâu dài.
 */
public class Bootstrap {

    public static final String DEFAULT_SERVICE_ID = "application_service";

    public static final Path DEFAULT_BOOTSTRAP_PATH =
            Path.of("./runtime/security/bootstrap.txt");

    private final String currentServiceId;
    private final Path bootstrapPath;
    private final BootstrapLoader loader;
    private final BootstrapValidator validator;

    public Bootstrap() {
        this(DEFAULT_SERVICE_ID, DEFAULT_BOOTSTRAP_PATH);
    }

    public Bootstrap(String currentServiceId, Path bootstrapPath) {
        this(currentServiceId, bootstrapPath, new BootstrapLoader(), new BootstrapValidator());
    }

    public Bootstrap(String currentServiceId, Path bootstrapPath,
                     BootstrapLoader loader, BootstrapValidator validator) {
        this.currentServiceId = Objects.requireNonNull(currentServiceId, "currentServiceId must not be null");
        this.bootstrapPath = Objects.requireNonNull(bootstrapPath, "bootstrapPath must not be null");
        this.loader = Objects.requireNonNull(loader, "loader must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    public boolean exists() {
        return Files.isRegularFile(bootstrapPath) && Files.isReadable(bootstrapPath);
    }

    public Certificate load() {
        BootstrapPayload payload = loader.load(bootstrapPath);
        validator.validate(currentServiceId, payload);
        return mapCertificate(payload);
    }

    public void delete() {
        try {
            Files.deleteIfExists(bootstrapPath);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to delete bootstrap file. Path: %s".formatted(bootstrapPath), e);
        }
    }

    public String currentServiceId() {
        return currentServiceId;
    }

    public Path bootstrapPath() {
        return bootstrapPath;
    }

    private Certificate mapCertificate(BootstrapPayload payload) {
        BootstrapPayload.Certificate cert = payload.certificate();
        return new Certificate(
                cert.id(),
                cert.publicCert(),
                cert.privateKey(),
                cert.serviceId(),
                payload.tokenId(),
                payload.refreshToken(),
                cert.issuedAtInstant(),
                cert.expiresAtInstant());
    }
}
