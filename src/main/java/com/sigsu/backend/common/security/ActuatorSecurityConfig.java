package com.sigsu.backend.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration(proxyBeanMethods = false)
@Profile("observability")
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(
            HttpSecurity http, PasswordEncoder encoder,
            @Value("${METRICS_USERNAME}") String username,
            @Value("${METRICS_PASSWORD}") String password) throws Exception {
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Se requieren credenciales técnicas de métricas no vacías");
        }
        var users = new InMemoryUserDetailsManager(User.withUsername(username)
                .password(encoder.encode(password)).roles("METRICS").build());
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"metrics\"");
            response.setStatus(401);
        };

        // Proveedor local a esta cadena: no crea usuarios ni autenticación Basic en la API.
        http.securityMatcher("/actuator", "/actuator/**")
                .authenticationManager(new ProviderManager(provider))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").hasRole("METRICS")
                        .anyRequest().denyAll());
        return http.build();
    }
}
