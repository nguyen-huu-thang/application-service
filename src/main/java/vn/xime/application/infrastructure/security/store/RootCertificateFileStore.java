package vn.xime.application.infrastructure.security.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

/**
 * Filesystem store for the root CA certificate (PEM).
 * Lưu trữ filesystem cho root CA certificate (PEM).
 *
 * Trust anchor đặt tại src/main/resources/security/trust/ca-cert.pem (cùng root CA platform).
 */
@Component
public class RootCertificateFileStore {

    private static final Path ROOT_CERT_PATH =
            Path.of("src/main/resources/security/trust/ca-cert.pem");

    public String load() {
        try {
            return Files.readString(ROOT_CERT_PATH);
        } catch (IOException exception) {
            throw new RuntimeException("failed to load root certificate", exception);
        }
    }

    public void save(String rootCertificate) {
        try {
            Path parent = ROOT_CERT_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(ROOT_CERT_PATH, rootCertificate);
        } catch (IOException exception) {
            throw new RuntimeException("failed to save root certificate", exception);
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(ROOT_CERT_PATH);
        } catch (IOException exception) {
            throw new RuntimeException("failed to delete root certificate", exception);
        }
    }

    public boolean exists() {
        return Files.exists(ROOT_CERT_PATH);
    }
}
