package com.sigsu.backend.common.security;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "observability"})
class ActuatorSecurityTests {
    private static final String USER = UUID.randomUUID().toString();
    private static final String PASSWORD = UUID.randomUUID().toString();
    @Autowired MockMvc mvc;
    @Autowired Environment environment;
    @Autowired WebEndpointsSupplier webEndpoints;
    @Autowired JwtService jwtService;
    @MockitoBean UsuarioRepository usuarios;

    @DynamicPropertySource
    static void credentials(DynamicPropertyRegistry registry) {
        registry.add("METRICS_USERNAME", () -> USER);
        registry.add("METRICS_PASSWORD", () -> PASSWORD);
    }

    @Test
    void healthIsPublicWithoutInternalDetails() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());
        mvc.perform(get("/actuator/health").with(httpBasic(USER, PASSWORD)))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void metricsRequireValidTechnicalCredentials() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic(USER, UUID.randomUUID().toString())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").with(httpBasic(USER, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("# HELP")))
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void sensitiveEndpointsAreNotExposedAndAreDeniedEvenToTechnicalUser() throws Exception {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,prometheus");
        assertThat(webEndpoints.getEndpoints()).extracting(endpoint -> endpoint.getEndpointId().toString())
                .containsExactlyInAnyOrder("health", "prometheus");
        for (String path : new String[]{"env", "configprops", "beans", "loggers", "heapdump", "shutdown", ""}) {
            mvc.perform(get("/actuator/" + path).with(httpBasic(USER, PASSWORD)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/actuator/shutdown").with(httpBasic(USER, PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void businessApiRejectsAnonymousInvalidJwtAndTechnicalBasic() throws Exception {
        mvc.perform(get("/api/usuarios/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/roles").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios/roles").with(httpBasic(USER, PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void businessJwtStillWorksAndRoleChecksStillApply() throws Exception {
        UsuarioEntity user = new UsuarioEntity();
        user.setCorreo("security-test@example.invalid");
        user.setRol(RolEnum.SOLICITANTE);
        when(usuarios.findByCorreo(user.getCorreo())).thenReturn(Optional.of(user));
        String token = jwtService.generateToken(user);
        mvc.perform(get("/api/usuarios/roles").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRemainsPublicAndSwaggerRemainsAvailable() throws Exception {
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
