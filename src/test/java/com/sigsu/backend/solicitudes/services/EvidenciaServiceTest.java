package com.sigsu.backend.solicitudes.services;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.storage.CloudinaryStorage;
import com.sigsu.backend.solicitudes.dtos.EvidenciaResponse;
import com.sigsu.backend.solicitudes.entities.EvidenciaEntity;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.EvidenciaRepository;
import com.sigsu.backend.solicitudes.repositories.SeguimientoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenciaServiceTest {

    private static final String URL = "https://res.cloudinary.com/demo/sigsu/evidencias/archivo";

    @Mock
    private SolicitudService solicitudService;

    @Mock
    private SeguimientoRepository seguimientoRepository;

    @Mock
    private EvidenciaRepository evidenciaRepository;

    @Mock
    private CloudinaryStorage cloudinaryStorage;

    @InjectMocks
    private EvidenciaService evidenciaService;

    @Test
    @DisplayName("UT-EVI-01 rechaza un archivo nulo o vacío")
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> evidenciaService.agregar(1, null, null, user()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El archivo de la evidencia es obligatorio");

        MockMultipartFile empty = new MockMultipartFile("archivo", "foto.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> evidenciaService.agregar(1, empty, null, user()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El archivo de la evidencia es obligatorio");
    }

    @Test
    @DisplayName("UT-EVI-02 rechaza una extensión no permitida")
    void rejectsInvalidExtension() {
        MockMultipartFile file = new MockMultipartFile("archivo", "evidencia.txt", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> evidenciaService.agregar(1, file, null, user()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Formato no permitido. Se aceptan jpg, jpeg, png, webp y pdf");
    }

    @Test
    @DisplayName("UT-EVI-03 rechaza un tipo MIME incoherente")
    void rejectsIncoherentContentType() {
        MockMultipartFile file = new MockMultipartFile("archivo", "evidencia.pdf", "text/plain", "texto".getBytes());

        assertThatThrownBy(() -> evidenciaService.agregar(1, file, null, user()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El tipo de archivo no corresponde a un formato permitido");
    }

    @Test
    @DisplayName("UT-EVI-04 acepta una extensión en mayúsculas")
    void acceptsUppercaseExtension() {
        MockMultipartFile file = new MockMultipartFile("archivo", "FOTO.PNG", "image/png", "png".getBytes());
        UsuarioEntity user = user();
        when(solicitudService.findVisible(1, user)).thenReturn(solicitud());
        when(cloudinaryStorage.upload(any(), anyString())).thenReturn(URL);
        when(evidenciaRepository.save(any(EvidenciaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvidenciaResponse response = evidenciaService.agregar(1, file, null, user);

        assertThat(response.getNombreArchivo()).isEqualTo("FOTO.PNG");
        assertThat(response.getArchivoUrl()).isEqualTo(URL);
    }

    @Test
    @DisplayName("UT-EVI-05 guarda una evidencia válida con sus datos")
    void savesValidEvidence() {
        MockMultipartFile file = new MockMultipartFile("archivo", "evidencia.pdf", "application/pdf", "pdf".getBytes());
        UsuarioEntity user = user();
        when(solicitudService.findVisible(1, user)).thenReturn(solicitud());
        when(cloudinaryStorage.upload(any(), anyString())).thenReturn(URL);
        when(evidenciaRepository.save(any(EvidenciaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvidenciaResponse response = evidenciaService.agregar(1, file, null, user);

        assertThat(response.getNombreArchivo()).isEqualTo("evidencia.pdf");
        assertThat(response.getTipoMime()).isEqualTo("application/pdf");
        assertThat(response.getTamanoBytes()).isEqualTo(3L);
        assertThat(response.getArchivoUrl()).isEqualTo(URL);
        assertThat(response.getSolicitudId()).isEqualTo(1);
        assertThat(response.getCargadoPorId()).isEqualTo(3);
    }

    private SolicitudEntity solicitud() {
        SolicitudEntity solicitud = new SolicitudEntity();
        ReflectionTestUtils.setField(solicitud, "id", 1);
        solicitud.setCodigo("SOL-2026-000001");
        solicitud.setTitulo("Falla de red");
        solicitud.setDescripcion("Sin conexión");
        solicitud.setUbicacion("Laboratorio 3");
        return solicitud;
    }

    private UsuarioEntity user() {
        UsuarioEntity user = new UsuarioEntity();
        ReflectionTestUtils.setField(user, "id", 3);
        user.setNombres("Ana");
        user.setApellidos("Perez");
        user.setCorreo("ana@sigsu.local");
        user.setPasswordHash("$2a$10$hash");
        user.setRol(RolEnum.SOLICITANTE);
        user.setActivo(true);
        return user;
    }
}
