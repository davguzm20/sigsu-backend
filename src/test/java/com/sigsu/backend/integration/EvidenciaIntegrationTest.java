package com.sigsu.backend.integration;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.common.exceptions.StorageException;
import com.sigsu.backend.common.storage.CloudinaryStorage;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvidenciaIntegrationTest extends IntegrationTestBase {

    private static final String URL = "https://res.cloudinary.com/demo/image/upload/sigsu/evidencias/solicitud.png";

    @MockitoBean
    private CloudinaryStorage cloudinaryStorage;

    @Test
    @DisplayName("INT-09 la subida válida devuelve una URL en sigsu/evidencias")
    void uploadsValidEvidence() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        SolicitudEntity solicitud = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        MockMultipartFile file = new MockMultipartFile("archivo", "evidencia.png", "image/png", "png".getBytes());
        when(cloudinaryStorage.upload(any(), anyString())).thenReturn(URL);

        mockMvc.perform(multipart("/api/solicitudes/" + solicitud.getId() + "/evidencias")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.archivoUrl").value(URL));

        assertThat(evidenciaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("INT-10 el PNG falso responde 502 y no guarda la evidencia")
    void rejectsFakePng() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        SolicitudEntity solicitud = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        MockMultipartFile file = new MockMultipartFile("archivo", "falso.png", "image/png", "no-es-png".getBytes());
        when(cloudinaryStorage.upload(any(), anyString()))
                .thenThrow(new StorageException("No se pudo guardar el archivo. Intente de nuevo en unos minutos"));

        mockMvc.perform(multipart("/api/solicitudes/" + solicitud.getId() + "/evidencias")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isBadGateway());

        assertThat(evidenciaRepository.count()).isZero();
    }
}
