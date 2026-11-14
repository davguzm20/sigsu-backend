package com.sigsu.backend.integration;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitudListIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("INT-01 el solicitante solo ve sus solicitudes")
    void listsOnlyOwnRequests() throws Exception {
        UsuarioEntity owner = createUser("owner", RolEnum.SOLICITANTE);
        UsuarioEntity other = createUser("other", RolEnum.SOLICITANTE);
        SolicitudEntity own = createSolicitud(owner, EstadoSolicitudEnum.REGISTRADA);
        createSolicitud(other, EstadoSolicitudEnum.REGISTRADA);

        mockMvc.perform(get("/api/solicitudes").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value(own.getCodigo()));
    }

    @Test
    @DisplayName("INT-02 el técnico solo ve las solicitudes asignadas activas")
    void listsOnlyActiveAssignmentsForTechnician() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        UsuarioEntity tecnico = createUser("tec", RolEnum.TECNICO);
        UsuarioEntity supervisor = createUser("sup", RolEnum.SUPERVISOR);
        SolicitudEntity assigned = createSolicitud(solicitante, EstadoSolicitudEnum.ASIGNADA);
        assign(assigned, tecnico, supervisor, null);
        createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);

        mockMvc.perform(get("/api/solicitudes").header(HttpHeaders.AUTHORIZATION, bearer(tecnico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value(assigned.getCodigo()));
    }

    @Test
    @DisplayName("INT-05 el rango de fechas incluye todo el día del extremo superior")
    void includesTheWholeUpperDay() throws Exception {
        UsuarioEntity supervisor = createUser("sup", RolEnum.SUPERVISOR);
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        SolicitudEntity first = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        SolicitudEntity second = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        setCreatedAt(first, LocalDateTime.of(2026, 11, 15, 0, 0, 1));
        setCreatedAt(second, LocalDateTime.of(2026, 11, 15, 23, 59, 59));

        mockMvc.perform(get("/api/solicitudes")
                        .param("desde", "2026-11-15")
                        .param("hasta", "2026-11-15")
                        .header(HttpHeaders.AUTHORIZATION, bearer(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("INT-06 el listado no duplica filas por las asignaciones")
    void doesNotDuplicateRowsWithAssignments() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        UsuarioEntity tecnico = createUser("tec", RolEnum.TECNICO);
        UsuarioEntity supervisor = createUser("sup", RolEnum.SUPERVISOR);
        SolicitudEntity solicitud = createSolicitud(solicitante, EstadoSolicitudEnum.ASIGNADA);
        assign(solicitud, tecnico, supervisor, LocalDateTime.now().minusDays(1));
        assign(solicitud, tecnico, supervisor, null);

        mockMvc.perform(get("/api/solicitudes").header(HttpHeaders.AUTHORIZATION, bearer(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value(solicitud.getCodigo()));
    }

    @Test
    @DisplayName("INT-07 el filtro sin asignar excluye las asignaciones activas")
    void filtersUnassignedRequests() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        UsuarioEntity tecnico = createUser("tec", RolEnum.TECNICO);
        UsuarioEntity supervisor = createUser("sup", RolEnum.SUPERVISOR);
        SolicitudEntity assigned = createSolicitud(solicitante, EstadoSolicitudEnum.ASIGNADA);
        assign(assigned, tecnico, supervisor, null);
        SolicitudEntity unassigned = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);

        mockMvc.perform(get("/api/solicitudes")
                        .param("sinAsignar", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(supervisor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value(unassigned.getCodigo()));
    }
}
