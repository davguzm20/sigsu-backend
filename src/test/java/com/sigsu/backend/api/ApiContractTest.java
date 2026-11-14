package com.sigsu.backend.api;

import com.sigsu.backend.asignaciones.services.AsignacionService;
import com.sigsu.backend.auth.dtos.LoginRequest;
import com.sigsu.backend.auth.dtos.LoginResponse;
import com.sigsu.backend.auth.dtos.UsuarioResponse;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.auth.services.AuthService;
import com.sigsu.backend.auth.services.UsuarioService;
import com.sigsu.backend.catalogos.services.CatalogoService;
import com.sigsu.backend.common.configs.CorsConfig;
import com.sigsu.backend.common.configs.CorsProperties;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import com.sigsu.backend.common.security.JwtService;
import com.sigsu.backend.common.security.RestSecurityErrorHandler;
import com.sigsu.backend.common.security.SecurityConfig;
import com.sigsu.backend.dashboard.services.DashboardService;
import com.sigsu.backend.solicitudes.services.EvidenciaService;
import com.sigsu.backend.solicitudes.services.SolicitudService;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfig.class, CorsConfig.class, RestSecurityErrorHandler.class})
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class ApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private SolicitudService solicitudService;

    @MockitoBean
    private AsignacionService asignacionService;

    @MockitoBean
    private CatalogoService catalogoService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private EvidenciaService evidenciaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("API-01 rechaza una petición protegida sin token")
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/solicitudes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API-02 rechaza un token malformado")
    void rejectsMalformedToken() throws Exception {
        when(jwtService.extractEmail("malformado")).thenThrow(new MalformedJwtException("token malformado"));

        mockMvc.perform(get("/api/solicitudes").header(HttpHeaders.AUTHORIZATION, "Bearer malformado"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API-03 rechaza un token firmado con otra clave")
    void rejectsTokenWithAnotherSignature() throws Exception {
        when(jwtService.extractEmail("otra-firma")).thenThrow(new SignatureException("firma inválida"));

        mockMvc.perform(get("/api/solicitudes").header(HttpHeaders.AUTHORIZATION, "Bearer otra-firma"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API-04 rechaza el token de un usuario desactivado")
    void rejectsInactiveUserToken() throws Exception {
        UsuarioEntity inactive = new UsuarioEntity();
        inactive.setCorreo("inactivo@sigsu.local");
        inactive.setActivo(false);
        when(jwtService.extractEmail("inactivo")).thenReturn("inactivo@sigsu.local");
        when(usuarioRepository.findByCorreo("inactivo@sigsu.local")).thenReturn(Optional.of(inactive));

        mockMvc.perform(get("/api/solicitudes").header(HttpHeaders.AUTHORIZATION, "Bearer inactivo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("API-05 rechaza el alta de usuario a un técnico")
    @WithMockUser(roles = "TECNICO")
    void rejectsUserCreationAsTechnician() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"correo\":\"ana@sigsu.local\",\"contrasena\":\"secreta123\",\"rol\":\"SOLICITANTE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-06 rechaza la clasificación a un solicitante")
    @WithMockUser(roles = "SOLICITANTE")
    void rejectsClassificationAsSolicitante() throws Exception {
        mockMvc.perform(put("/api/solicitudes/1/clasificacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoriaId\":1,\"prioridadId\":1,\"areaId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-07 rechaza el registro de solicitud a un técnico")
    @WithMockUser(roles = "TECNICO")
    void rejectsRequestCreationAsTechnician() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Falla\",\"descripcion\":\"Sin conexión\",\"ubicacion\":\"Laboratorio\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-08 rechaza la creación de categoría a un supervisor")
    @WithMockUser(roles = "SUPERVISOR")
    void rejectsCategoryCreationAsSupervisor() throws Exception {
        mockMvc.perform(post("/api/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"REDES\",\"nombre\":\"Redes\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-09 rechaza el dashboard a un solicitante")
    @WithMockUser(roles = "SOLICITANTE")
    void rejectsDashboardAsSolicitante() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-10 rechaza la asignación a un solicitante")
    @WithMockUser(roles = "SOLICITANTE")
    void rejectsAssignmentAsSolicitante() throws Exception {
        mockMvc.perform(post("/api/solicitudes/1/asignaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-11 rechaza la eliminación de usuario a un supervisor")
    @WithMockUser(roles = "SUPERVISOR")
    void rejectsUserDeletionAsSupervisor() throws Exception {
        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-12 rechaza el listado de usuarios a un supervisor")
    @WithMockUser(roles = "SUPERVISOR")
    void rejectsUserListingAsSupervisor() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("API-13 el login público devuelve token, tipo y expiración")
    void returnsTokenOnLogin() throws Exception {
        UsuarioResponse user = new UsuarioResponse(1, "Ana", "Perez", "ana@sigsu.local", "SOLICITANTE", null, true);
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new LoginResponse("token-de-prueba", "Bearer", 3_600_000L, user));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"ana@sigsu.local\",\"contrasena\":\"secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-de-prueba"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEn").value(3_600_000L));
    }

    @Test
    @DisplayName("API-14 responde 404 para una solicitud inexistente y 400 para un id inválido")
    @WithMockUser(roles = "SOLICITANTE")
    void returnsNotFoundForUnknownRequest() throws Exception {
        when(solicitudService.findDetail(eq(9999), isNull()))
                .thenThrow(new ResourceNotFoundException("Solicitud no encontrada"));

        mockMvc.perform(get("/api/solicitudes/9999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/solicitudes/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-15 rechaza un cuerpo de solicitud inválido")
    @WithMockUser(roles = "SOLICITANTE")
    void rejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Datos inválidos en la solicitud"));
    }

    @Test
    @DisplayName("API-16 rechaza un título de 151 caracteres")
    @WithMockUser(roles = "SOLICITANTE")
    void rejectsTitleTooLong() throws Exception {
        String title = "a".repeat(151);

        mockMvc.perform(post("/api/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"" + title + "\",\"descripcion\":\"Descripción\",\"ubicacion\":\"Laboratorio\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value("El titulo no puede superar los 150 caracteres"));
    }

    @Test
    @DisplayName("API-17 rechaza una contraseña de 7 caracteres")
    @WithMockUser(roles = "ADMINISTRADOR")
    void rejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"correo\":\"ana@sigsu.local\",\"contrasena\":\"1234567\",\"rol\":\"SOLICITANTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-18 rechaza un estado inexistente")
    @WithMockUser(roles = "SUPERVISOR")
    void rejectsUnknownState() throws Exception {
        mockMvc.perform(get("/api/solicitudes?estado=INVENTADO"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-19 rechaza una evidencia con extensión .txt")
    @WithMockUser(roles = "SOLICITANTE")
    void rejectsTxtEvidence() throws Exception {
        MockMultipartFile file = new MockMultipartFile("archivo", "evidencia.txt", "text/plain", "texto".getBytes());
        when(evidenciaService.agregar(eq(1), any(), isNull(), isNull()))
                .thenThrow(new BusinessRuleException("Formato no permitido. Se aceptan jpg, jpeg, png, webp y pdf"));

        mockMvc.perform(multipart("/api/solicitudes/1/evidencias").file(file))
                .andExpect(status().isUnprocessableContent());
    }

}
