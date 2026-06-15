package vn.xime.application.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Minimal HTTP security: actuator is the only HTTP surface; everything else is denied.
 * Bảo mật HTTP tối giản: actuator là bề mặt HTTP duy nhất; còn lại đều chặn.
 *
 * Application Service KHÔNG có JWT/credential và KHÔNG nằm trong login flow. API nội bộ
 * đi qua gRPC + mTLS (xử lý ở tầng gRPC, không phải Spring Security). REST chỉ phục vụ
 * health/metrics. Vì vậy không có JWT filter như user-service.
 */
@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().denyAll());

        return http.build();
    }
}
