package com.sigsu.backend.integration;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JwtIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("INT-12 el token válido autentica y el inválido responde 401")
    void filtersJwtWithRealSecurity() throws Exception {
        UsuarioEntity user = createUser("jwt", RolEnum.SOLICITANTE);

        mockMvc.perform(get("/api/auth/perfil").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value(user.getCorreo()));

        mockMvc.perform(get("/api/auth/perfil").header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
