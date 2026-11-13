package com.sigsu.backend.asignaciones.services;

import com.sigsu.backend.asignaciones.dtos.AsignacionResponse;
import com.sigsu.backend.asignaciones.dtos.AsignarTecnicoRequest;
import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.exceptions.ConflictException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsignacionServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @InjectMocks
    private AsignacionService asignacionService;

    @Test
    @DisplayName("UT-ASG-01 rechaza asignar una solicitud sin categoría")
    void rejectsAssignmentWithoutCategory() {
        SolicitudEntity solicitud = solicitud();
        solicitud.setCategoria(null);
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> asignacionService.asignar(1, request(2), supervisor()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La solicitud debe tener categoría y área antes de asignar un técnico");

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-ASG-02 rechaza asignar una solicitud sin área")
    void rejectsAssignmentWithoutArea() {
        SolicitudEntity solicitud = solicitud();
        solicitud.setArea(null);
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> asignacionService.asignar(1, request(2), supervisor()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La solicitud debe tener categoría y área antes de asignar un técnico");

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-ASG-03 rechaza una segunda asignación activa")
    void rejectsSecondActiveAssignment() {
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud()));
        when(asignacionRepository.existsBySolicitudIdAndFinalizadaEnIsNull(1)).thenReturn(true);

        assertThatThrownBy(() -> asignacionService.asignar(1, request(2), supervisor()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La solicitud ya tiene un técnico asignado");

        verify(asignacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-ASG-04 rechaza una solicitud inexistente")
    void rejectsUnknownRequest() {
        when(solicitudRepository.findByIdForUpdate(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asignacionService.asignar(9999, request(2), supervisor()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Solicitud no encontrada");
    }

    @Test
    @DisplayName("UT-ASG-05 rechaza un técnico inexistente")
    void rejectsUnknownTechnician() {
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud()));
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> asignacionService.asignar(1, request(99), supervisor()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Técnico no encontrado");
    }

    @Test
    @DisplayName("UT-ASG-06 rechaza un usuario que no es técnico")
    void rejectsUserWithoutTechnicianRole() {
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud()));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(user(2, RolEnum.SOLICITANTE, true)));

        assertThatThrownBy(() -> asignacionService.asignar(1, request(2), supervisor()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El usuario indicado no tiene el rol de técnico");
    }

    @Test
    @DisplayName("UT-ASG-07 rechaza un técnico desactivado")
    void rejectsInactiveTechnician() {
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud()));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(user(2, RolEnum.TECNICO, false)));

        assertThatThrownBy(() -> asignacionService.asignar(1, request(2), supervisor()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El técnico indicado está desactivado");
    }

    @Test
    @DisplayName("UT-ASG-08 guarda una asignación válida con fecha y autor")
    void savesValidAssignment() {
        SolicitudEntity solicitud = solicitud();
        UsuarioEntity tecnico = user(2, RolEnum.TECNICO, true);
        UsuarioEntity supervisor = supervisor();
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(tecnico));
        when(asignacionRepository.save(any(AsignacionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AsignacionResponse response = asignacionService.asignar(1, request(2), supervisor);

        assertThat(response.getSolicitudId()).isEqualTo(1);
        assertThat(response.getSolicitudCodigo()).isEqualTo("SOL-2026-000001");
        assertThat(response.getTecnicoId()).isEqualTo(2);
        assertThat(response.getTecnicoNombre()).isEqualTo("Luis Gomez");
        assertThat(response.getAsignadoPorId()).isEqualTo(3);
        assertThat(response.getAsignadaEn()).isNotNull();
    }

    private SolicitudEntity solicitud() {
        CategoriaEntity categoria = new CategoriaEntity();
        categoria.setCodigo("REDES");
        categoria.setNombre("Redes");
        AreaEntity area = new AreaEntity();
        area.setCodigo("SISTEMAS");
        area.setNombre("Sistemas");

        SolicitudEntity solicitud = new SolicitudEntity();
        ReflectionTestUtils.setField(solicitud, "id", 1);
        solicitud.setCodigo("SOL-2026-000001");
        solicitud.setTitulo("Falla de red");
        solicitud.setDescripcion("Sin conexión");
        solicitud.setUbicacion("Laboratorio 3");
        solicitud.setCategoria(categoria);
        solicitud.setArea(area);
        return solicitud;
    }

    private AsignarTecnicoRequest request(Integer tecnicoId) {
        AsignarTecnicoRequest request = new AsignarTecnicoRequest();
        request.setTecnicoId(tecnicoId);
        return request;
    }

    private UsuarioEntity supervisor() {
        return user(3, RolEnum.SUPERVISOR, true);
    }

    private UsuarioEntity user(Integer id, RolEnum rol, boolean activo) {
        UsuarioEntity user = new UsuarioEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setNombres(rol == RolEnum.TECNICO ? "Luis" : "Marta");
        user.setApellidos(rol == RolEnum.TECNICO ? "Gomez" : "Diaz");
        user.setCorreo("usuario" + id + "@sigsu.local");
        user.setPasswordHash("$2a$10$hash");
        user.setRol(rol);
        user.setActivo(activo);
        return user;
    }
}
