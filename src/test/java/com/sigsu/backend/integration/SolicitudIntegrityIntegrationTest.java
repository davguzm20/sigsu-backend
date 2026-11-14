package com.sigsu.backend.integration;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitudIntegrityIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("INT-03 el código es único y el índice rechaza un duplicado")
    void keepsTheCodeUnique() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        String prefix = "SOL-" + LocalDate.now().getYear() + "-";
        String body = "{\"titulo\":\"Falla\",\"descripcion\":\"Sin conexión\",\"ubicacion\":\"Laboratorio\"}";

        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(prefix + "000001"));

        mockMvc.perform(post("/api/solicitudes")
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(prefix + "000002"));

        SolicitudEntity duplicate = new SolicitudEntity();
        duplicate.setCodigo(prefix + "000001");
        duplicate.setSolicitante(solicitante);
        duplicate.setEstado(EstadoSolicitudEnum.REGISTRADA);
        duplicate.setTitulo("Duplicada");
        duplicate.setDescripcion("Duplicada");
        duplicate.setUbicacion("Laboratorio");

        assertThatThrownBy(() -> solicitudRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("INT-04 no se puede borrar una categoría en uso")
    void rejectsDeletingACategoryInUse() throws Exception {
        UsuarioEntity admin = createUser("admin", RolEnum.ADMINISTRADOR);
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        AreaEntity area = createArea();
        CategoriaEntity categoria = createCategoria();
        SolicitudEntity solicitud = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        solicitud.setArea(area);
        solicitud.setCategoria(categoria);
        solicitudRepository.save(solicitud);

        mockMvc.perform(delete("/api/categorias/" + categoria.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isConflict());
    }
}
