package com.sigsu.backend.solicitudes.services;

import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.catalogos.repositories.CategoriaRepository;
import com.sigsu.backend.catalogos.repositories.PrioridadRepository;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.exceptions.ForbiddenException;
import com.sigsu.backend.solicitudes.dtos.CambiarEstadoRequest;
import com.sigsu.backend.solicitudes.dtos.CrearSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.SolicitudResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.HistorialEstadoEntity;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.HistorialEstadoRepository;
import com.sigsu.backend.solicitudes.repositories.SeguimientoRepository;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private SeguimientoRepository seguimientoRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private HistorialEstadoRepository historialEstadoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private PrioridadRepository prioridadRepository;

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private SolicitudService solicitudService;

    @Test
    @DisplayName("UT-SOL-01 mueve de REGISTRADA a EN_EVALUACION y registra historial")
    void movesFromRegistradaToEnEvaluacion() {
        SolicitudEntity solicitud = given(EstadoSolicitudEnum.REGISTRADA);

        SolicitudResponse response = solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_EVALUACION),
                user(RolEnum.SUPERVISOR));

        assertThat(response.getEstado()).isEqualTo("EN_EVALUACION");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEnum.EN_EVALUACION);
        verify(historialEstadoRepository).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-02 mueve de EN_EVALUACION a ASIGNADA con asignación activa")
    void movesFromEnEvaluacionToAsignada() {
        SolicitudEntity solicitud = given(EstadoSolicitudEnum.EN_EVALUACION);
        when(asignacionRepository.existsBySolicitudIdAndFinalizadaEnIsNull(1)).thenReturn(true);

        SolicitudResponse response = solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.ASIGNADA),
                user(RolEnum.SUPERVISOR));

        assertThat(response.getEstado()).isEqualTo("ASIGNADA");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEnum.ASIGNADA);
    }

    @Test
    @DisplayName("UT-SOL-03 mueve de ASIGNADA a EN_ATENCION con el técnico")
    void movesFromAsignadaToEnAtencion() {
        SolicitudEntity solicitud = given(EstadoSolicitudEnum.ASIGNADA);
        when(asignacionRepository.existsBySolicitudIdAndFinalizadaEnIsNull(1)).thenReturn(true);

        SolicitudResponse response = solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_ATENCION),
                user(RolEnum.TECNICO));

        assertThat(response.getEstado()).isEqualTo("EN_ATENCION");
        assertThat(solicitud.getEstado()).isEqualTo(EstadoSolicitudEnum.EN_ATENCION);
    }

    @Test
    @DisplayName("UT-SOL-04 mueve de EN_ATENCION a RESUELTA y escribe resueltaEn")
    void movesFromEnAtencionToResuelta() {
        SolicitudEntity solicitud = given(EstadoSolicitudEnum.EN_ATENCION);
        CambiarEstadoRequest request = request(EstadoSolicitudEnum.RESUELTA);
        request.setSolucion("Se cambió el switch");

        SolicitudResponse response = solicitudService.cambiarEstado(1, request, user(RolEnum.TECNICO));

        assertThat(response.getEstado()).isEqualTo("RESUELTA");
        assertThat(solicitud.getResueltaEn()).isNotNull();
    }

    @Test
    @DisplayName("UT-SOL-05 mueve de RESUELTA a CERRADA y escribe cerradaEn")
    void movesFromResueltaToCerrada() {
        SolicitudEntity solicitud = given(EstadoSolicitudEnum.RESUELTA);

        SolicitudResponse response = solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.CERRADA),
                user(RolEnum.SUPERVISOR));

        assertThat(response.getEstado()).isEqualTo("CERRADA");
        assertThat(solicitud.getCerradaEn()).isNotNull();
    }

    @Test
    @DisplayName("UT-SOL-06 rechaza el salto de REGISTRADA a CERRADA")
    void rejectsJumpFromRegistradaToCerrada() {
        given(EstadoSolicitudEnum.REGISTRADA);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.CERRADA),
                user(RolEnum.SUPERVISOR)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede pasar de REGISTRADA a CERRADA");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-07 rechaza mover una solicitud CERRADA")
    void rejectsTransitionFromCerrada() {
        given(EstadoSolicitudEnum.CERRADA);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_EVALUACION),
                user(RolEnum.SUPERVISOR)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede pasar de CERRADA a EN_EVALUACION");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-08 rechaza el retroceso de RESUELTA a EN_ATENCION")
    void rejectsBackwardTransitionFromResuelta() {
        given(EstadoSolicitudEnum.RESUELTA);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_ATENCION),
                user(RolEnum.TECNICO)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede pasar de RESUELTA a EN_ATENCION");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-09 rechaza el salto de EN_EVALUACION a EN_ATENCION")
    void rejectsJumpFromEnEvaluacionToEnAtencion() {
        given(EstadoSolicitudEnum.EN_EVALUACION);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_ATENCION),
                user(RolEnum.TECNICO)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No se puede pasar de EN_EVALUACION a EN_ATENCION");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-10 rechaza al técnico en la transición del supervisor")
    void rejectsSupervisorTransitionWithTechnicianRole() {
        given(EstadoSolicitudEnum.REGISTRADA);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_EVALUACION),
                user(RolEnum.TECNICO)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Su rol no puede mover la solicitud de REGISTRADA a EN_EVALUACION");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-11 rechaza al supervisor en la transición del técnico")
    void rejectsTechnicianTransitionWithSupervisorRole() {
        given(EstadoSolicitudEnum.ASIGNADA);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.EN_ATENCION),
                user(RolEnum.SUPERVISOR)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Su rol no puede mover la solicitud de ASIGNADA a EN_ATENCION");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-12 rechaza al administrador en el cierre")
    void rejectsAdministratorClosing() {
        given(EstadoSolicitudEnum.RESUELTA);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.CERRADA),
                user(RolEnum.ADMINISTRADOR)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Su rol no puede mover la solicitud de RESUELTA a CERRADA");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-13 rechaza resolver sin solución")
    void rejectsResolutionWithoutSolution() {
        given(EstadoSolicitudEnum.EN_ATENCION);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.RESUELTA),
                user(RolEnum.TECNICO)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Para marcar la solicitud como resuelta hay que registrar la solución");

        verify(historialEstadoRepository, never()).save(any(HistorialEstadoEntity.class));
    }

    @Test
    @DisplayName("UT-SOL-14 rechaza asignar sin asignación activa")
    void rejectsAssignmentWithoutActiveAssignment() {
        given(EstadoSolicitudEnum.EN_EVALUACION);
        when(asignacionRepository.existsBySolicitudIdAndFinalizadaEnIsNull(1)).thenReturn(false);

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1, request(EstadoSolicitudEnum.ASIGNADA),
                user(RolEnum.SUPERVISOR)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La solicitud necesita un técnico asignado para pasar a ese estado");
    }

    @Test
    @DisplayName("UT-SOL-15 escribe la solución al resolver")
    void writesSolutionWhenResolving() {
        SolicitudEntity solicitud = given(EstadoSolicitudEnum.EN_ATENCION);
        CambiarEstadoRequest request = request(EstadoSolicitudEnum.RESUELTA);
        request.setSolucion("Se cambió el switch");

        solicitudService.cambiarEstado(1, request, user(RolEnum.TECNICO));

        assertThat(solicitud.getSolucion()).isEqualTo("Se cambió el switch");
        assertThat(solicitud.getResueltaEn()).isNotNull();
    }

    @Test
    @DisplayName("UT-SOL-16 cierra con historial de origen, destino y actor")
    void writesHistoryWhenClosing() {
        given(EstadoSolicitudEnum.RESUELTA);
        UsuarioEntity supervisor = user(RolEnum.SUPERVISOR);
        CambiarEstadoRequest request = request(EstadoSolicitudEnum.CERRADA);
        request.setObservacion("Cierre conforme");

        solicitudService.cambiarEstado(1, request, supervisor);

        ArgumentCaptor<HistorialEstadoEntity> captor = ArgumentCaptor.forClass(HistorialEstadoEntity.class);
        verify(historialEstadoRepository).save(captor.capture());
        HistorialEstadoEntity historial = captor.getValue();
        assertThat(historial.getEstadoOrigen()).isEqualTo(EstadoSolicitudEnum.RESUELTA);
        assertThat(historial.getEstadoDestino()).isEqualTo(EstadoSolicitudEnum.CERRADA);
        assertThat(historial.getCambiadoPor()).isSameAs(supervisor);
        assertThat(historial.getCambiadoEn()).isNotNull();
        assertThat(historial.getObservacion()).isEqualTo("Cierre conforme");
    }

    @Test
    @DisplayName("UT-ACC-01 el solicitante dueño consulta su solicitud")
    void ownerCanViewOwnRequest() {
        SolicitudEntity solicitud = solicitud(EstadoSolicitudEnum.REGISTRADA);
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(solicitud));

        SolicitudResponse response = solicitudService.findDetail(1, user(1, RolEnum.SOLICITANTE));

        assertThat(response.getCodigo()).isEqualTo("SOL-2026-000001");
    }

    @Test
    @DisplayName("UT-ACC-02 el solicitante ajeno recibe 403")
    void rejectsForeignRequest() {
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(solicitud(EstadoSolicitudEnum.REGISTRADA)));

        assertThatThrownBy(() -> solicitudService.findDetail(1, user(2, RolEnum.SOLICITANTE)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("No tiene permiso para consultar esta solicitud");
    }

    @Test
    @DisplayName("UT-ACC-03 el técnico asignado consulta la solicitud")
    void assignedTechnicianCanViewRequest() {
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(solicitud(EstadoSolicitudEnum.ASIGNADA)));
        when(asignacionRepository.existsBySolicitudIdAndTecnicoIdAndFinalizadaEnIsNull(1, 10)).thenReturn(true);

        SolicitudResponse response = solicitudService.findDetail(1, user(10, RolEnum.TECNICO));

        assertThat(response.getCodigo()).isEqualTo("SOL-2026-000001");
    }

    @Test
    @DisplayName("UT-ACC-04 el técnico no asignado recibe 403")
    void rejectsRequestForUnassignedTechnician() {
        when(solicitudRepository.findById(1)).thenReturn(Optional.of(solicitud(EstadoSolicitudEnum.ASIGNADA)));
        when(asignacionRepository.existsBySolicitudIdAndTecnicoIdAndFinalizadaEnIsNull(1, 10)).thenReturn(false);

        assertThatThrownBy(() -> solicitudService.findDetail(1, user(10, RolEnum.TECNICO)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("No tiene permiso para consultar esta solicitud");
    }

    @Test
    @DisplayName("UT-SOL-17 genera el consecutivo del año")
    void generatesYearSequence() {
        String prefix = "SOL-" + LocalDate.now().getYear() + "-";
        SolicitudEntity previous = new SolicitudEntity();
        previous.setCodigo(prefix + "000001");
        when(solicitudRepository.findFirstByCodigoStartingWithOrderByCodigoDesc(prefix))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(previous));
        when(solicitudRepository.save(any(SolicitudEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitudResponse first = solicitudService.register(createRequest(), user(1, RolEnum.SOLICITANTE));
        SolicitudResponse second = solicitudService.register(createRequest(), user(1, RolEnum.SOLICITANTE));

        assertThat(first.getCodigo()).isEqualTo(prefix + "000001");
        assertThat(second.getCodigo()).isEqualTo(prefix + "000002");
    }

    private SolicitudEntity given(EstadoSolicitudEnum estado) {
        SolicitudEntity solicitud = solicitud(estado);
        when(solicitudRepository.findByIdForUpdate(1)).thenReturn(Optional.of(solicitud));
        return solicitud;
    }

    private SolicitudEntity solicitud(EstadoSolicitudEnum estado) {
        SolicitudEntity solicitud = new SolicitudEntity();
        ReflectionTestUtils.setField(solicitud, "id", 1);
        solicitud.setCodigo("SOL-2026-000001");
        solicitud.setTitulo("Falla de red");
        solicitud.setDescripcion("Sin conexión");
        solicitud.setUbicacion("Laboratorio 3");
        solicitud.setEstado(estado);
        solicitud.setSolicitante(user(RolEnum.SOLICITANTE));
        return solicitud;
    }

    private CambiarEstadoRequest request(EstadoSolicitudEnum estado) {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setEstado(estado);
        return request;
    }

    private CrearSolicitudRequest createRequest() {
        CrearSolicitudRequest request = new CrearSolicitudRequest();
        request.setTitulo("Falla de red");
        request.setDescripcion("Sin conexión");
        request.setUbicacion("Laboratorio 3");
        return request;
    }

    private UsuarioEntity user(RolEnum rol) {
        return user(1, rol);
    }

    private UsuarioEntity user(Integer id, RolEnum rol) {
        UsuarioEntity user = new UsuarioEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setNombres("Ana");
        user.setApellidos("Perez");
        user.setCorreo("ana@sigsu.local");
        user.setPasswordHash("$2a$10$hash");
        user.setRol(rol);
        user.setActivo(true);
        return user;
    }
}
