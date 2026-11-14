package com.sigsu.backend.integration;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.common.exceptions.StorageException;
import com.sigsu.backend.common.storage.CloudinaryStorage;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EvidenciaIntegrationTest extends IntegrationTestBase {

    private static final String URL = "https://res.cloudinary.com/demo/image/upload/sigsu/evidencias/solicitud.png";
    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @MockitoBean
    private CloudinaryStorage cloudinaryStorage;

    @Test
    @DisplayName("INT-09 la subida válida devuelve una URL en sigsu/evidencias")
    void uploadsValidEvidence() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        SolicitudEntity solicitud = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        MockMultipartFile file = new MockMultipartFile("archivo", "evidencia.png", "image/png", PNG_BYTES);
        when(cloudinaryStorage.upload(any(), anyString())).thenReturn(URL);

        mockMvc.perform(multipart("/api/solicitudes/" + solicitud.getId() + "/evidencias")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.archivoUrl").value(URL))
                .andExpect(jsonPath("$.nombreArchivo").value("evidencia.png"))
                .andExpect(jsonPath("$.tipoMime").value("image/png"))
                .andExpect(jsonPath("$.tamanoBytes").value(PNG_BYTES.length));

        ArgumentCaptor<String> publicId = ArgumentCaptor.forClass(String.class);
        verify(cloudinaryStorage).upload(any(), publicId.capture());
        assertThat(publicId.getValue()).startsWith("solicitud_" + solicitud.getId() + "_");
        assertThat(evidenciaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("INT-10 el PNG falso responde 502 y no guarda la evidencia")
    void rejectsFakePng() throws Exception {
        UsuarioEntity solicitante = createUser("sol", RolEnum.SOLICITANTE);
        SolicitudEntity solicitud = createSolicitud(solicitante, EstadoSolicitudEnum.REGISTRADA);
        MockMultipartFile file = new MockMultipartFile("archivo", "falso.png", "image/png", "no-es-png".getBytes());
        when(cloudinaryStorage.upload(any(), anyString())).thenAnswer(invocation -> {
            MultipartFile uploaded = invocation.getArgument(0);
            if (!isPng(uploaded.getBytes())) {
                throw new StorageException("No se pudo guardar el archivo. Intente de nuevo en unos minutos");
            }
            return URL;
        });

        mockMvc.perform(multipart("/api/solicitudes/" + solicitud.getId() + "/evidencias")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(solicitante)))
                .andExpect(status().isBadGateway());

        assertThat(evidenciaRepository.count()).isZero();
    }

    private static boolean isPng(byte[] bytes) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (bytes.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
