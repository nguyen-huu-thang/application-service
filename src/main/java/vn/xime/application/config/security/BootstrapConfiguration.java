package vn.xime.application.config.security;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

import vn.xime.application.infrastructure.security.bootstrap.Bootstrap;
import vn.xime.application.infrastructure.security.bootstrap.BootstrapLoader;
import vn.xime.application.infrastructure.security.bootstrap.BootstrapValidator;

/**
 * Wires bootstrap beans (loader, validator, Bootstrap) from application.bootstrap.* props.
 * Wire các bean bootstrap (loader, validator, Bootstrap) từ props application.bootstrap.*.
 */
@Configuration
@EnableConfigurationProperties(BootstrapConfiguration.BootstrapProperties.class)
public class BootstrapConfiguration {

    @Bean
    BootstrapLoader bootstrapLoader() {
        return new BootstrapLoader();
    }

    @Bean
    BootstrapValidator bootstrapValidator() {
        return new BootstrapValidator();
    }

    @Bean
    Bootstrap bootstrap(BootstrapProperties properties,
                        BootstrapLoader loader,
                        BootstrapValidator validator) {
        return new Bootstrap(
                properties.getServiceId(),
                Path.of(properties.getPath()),
                loader,
                validator);
    }

    @Getter
    @Setter
    @ConfigurationProperties(prefix = "application.bootstrap")
    public static class BootstrapProperties {

        /** Default: application_service */
        private String serviceId = "application_service";

        /** Default: ./runtime/security/bootstrap.txt */
        private String path = "./runtime/security/bootstrap.txt";
    }
}
