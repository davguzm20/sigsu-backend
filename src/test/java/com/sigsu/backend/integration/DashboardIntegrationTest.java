package com.sigsu.backend.integration;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("INT-08 el dashboard devuelve los nueve indicadores con datos reales")
    void returnsTheNineIndicators() throws Exception {
        UsuarioEntity supervisor = createUser("sup", RolEnum.SUPERVISOR);
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        UsuarioEntity techOne = createUser("tec1", RolEnum.TECNICO);
        UsuarioEntity techTwo = createUser("tec2", RolEnum.TECNICO);
        CategoriaEntity categoryA = createCategoria();
        CategoriaEntity categoryB = createCategoria();
        PrioridadEntity high = createPrioridad();
        PrioridadEntity medium = createPrioridad();
        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 8, 0);

        SolicitudEntity resolved = createSolicitud(solicitante, EstadoSolicitudEnum.RESUELTA);
        resolved.setCategoria(categoryA);
        resolved.setPrioridad(high);
        solicitudRepository.save(resolved);
        assign(resolved, techOne, supervisor, null);
        resolved.setResueltaEn(base.plusHours(10));
        solicitudRepository.save(resolved);
        setCreatedAt(resolved, base);

        SolicitudEntity closed = createSolicitud(solicitante, EstadoSolicitudEnum.CERRADA);
        closed.setCategoria(categoryA);
        closed.setPrioridad(high);
        solicitudRepository.save(closed);
        assign(closed, techOne, supervisor, null);
        closed.setResueltaEn(base.plusHours(20));
        solicitudRepository.save(closed);
        setCreatedAt(closed, base);

        SolicitudEntity inProgress = createSolicitud(solicitante, EstadoSolicitudEnum.EN_ATENCION);
        inProgress.setCategoria(categoryB);
        inProgress.setPrioridad(medium);
        inProgress.setFechaLimite(LocalDateTime.now().minusHours(1));
        solicitudRepository.save(inProgress);
        assign(inProgress, techTwo, supervisor, null);
        setCreatedAt(inProgress, base);

        SolicitudEntity registered = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        registered.setCategoria(categoryB);
        registered.setPrioridad(medium);
        solicitudRepository.save(registered);
        setCreatedAt(registered, base);

        mockMvc.perform(get("/api/dashboard").header(HttpHeaders.AUTHORIZATION, bearer(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.pendientes").value(3))
                .andExpect(jsonPath("$.atendidas").value(3))
                .andExpect(jsonPath("$.vencidas").value(1))
                .andExpect(jsonPath("$.porcentajeResuelto").value(50.0))
                .andExpect(jsonPath("$.tiempoPromedioAtencionHoras").value(15.0))
                .andExpect(jsonPath("$.porCategoria.length()").value(2))
                .andExpect(jsonPath("$.porCategoria[0].total").value(2))
                .andExpect(jsonPath("$.porPrioridad.length()").value(2))
                .andExpect(jsonPath("$.porPrioridad[0].total").value(2))
                .andExpect(jsonPath("$.porResponsable.length()").value(2))
                .andExpect(jsonPath("$.porResponsable[0].total").value(2));
    }
}
