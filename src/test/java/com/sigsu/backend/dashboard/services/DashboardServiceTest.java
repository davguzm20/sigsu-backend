package com.sigsu.backend.dashboard.services;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import com.sigsu.backend.dashboard.dtos.DashboardResponse;
import com.sigsu.backend.dashboard.dtos.IndicadorResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("UT-DSH-01 devuelve todo en cero sin solicitudes")
    void returnsZerosWithoutRequests() {
        when(solicitudRepository.findAll(anySpecification(), any(Sort.class))).thenReturn(List.of());

        DashboardResponse response = dashboardService.build(null, null, null, null);

        assertThat(response.getTotal()).isZero();
        assertThat(response.getPendientes()).isZero();
        assertThat(response.getAtendidas()).isZero();
        assertThat(response.getVencidas()).isZero();
        assertThat(response.getPorcentajeResuelto()).isZero();
        assertThat(response.getTiempoPromedioAtencionHoras()).isZero();
        assertThat(response.getPorCategoria()).isEmpty();
        assertThat(response.getPorPrioridad()).isEmpty();
        assertThat(response.getPorResponsable()).isEmpty();
    }

    @Test
    @DisplayName("UT-DSH-02 cuenta pendientes y atendidas por estado")
    void countsPendingAndAttended() {
        when(solicitudRepository.findAll(anySpecification(), any(Sort.class))).thenReturn(List.of(
                solicitud(1, EstadoSolicitudEnum.REGISTRADA),
                solicitud(2, EstadoSolicitudEnum.EN_EVALUACION),
                solicitud(3, EstadoSolicitudEnum.ASIGNADA),
                solicitud(4, EstadoSolicitudEnum.EN_ATENCION),
                solicitud(5, EstadoSolicitudEnum.RESUELTA),
                solicitud(6, EstadoSolicitudEnum.CERRADA)));

        DashboardResponse response = dashboardService.build(null, null, null, null);

        assertThat(response.getTotal()).isEqualTo(6);
        assertThat(response.getPendientes()).isEqualTo(5);
        assertThat(response.getAtendidas()).isEqualTo(3);
        assertThat(response.getPorcentajeResuelto()).isEqualTo(33.3);
    }

    @Test
    @DisplayName("UT-DSH-03 solo cuenta como vencida la abierta con plazo pasado")
    void countsOnlyOpenOverdueRequests() {
        SolicitudEntity open = solicitud(1, EstadoSolicitudEnum.EN_ATENCION);
        open.setFechaLimite(LocalDateTime.now().minusHours(1));
        SolicitudEntity closed = solicitud(2, EstadoSolicitudEnum.CERRADA);
        closed.setFechaLimite(LocalDateTime.now().minusHours(1));
        SolicitudEntity withoutDeadline = solicitud(3, EstadoSolicitudEnum.REGISTRADA);

        when(solicitudRepository.findAll(anySpecification(), any(Sort.class)))
                .thenReturn(List.of(open, closed, withoutDeadline));

        DashboardResponse response = dashboardService.build(null, null, null, null);

        assertThat(response.getVencidas()).isEqualTo(1);
    }

    @Test
    @DisplayName("UT-DSH-04 calcula porcentaje resuelto y promedio de atención")
    void calculatesResolvedPercentageAndAverageTime() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 8, 0);
        SolicitudEntity first = solicitud(1, EstadoSolicitudEnum.RESUELTA);
        creadoEn(first, base);
        first.setResueltaEn(base.plusHours(10));
        SolicitudEntity second = solicitud(2, EstadoSolicitudEnum.CERRADA);
        creadoEn(second, base);
        second.setResueltaEn(base.plusHours(20));

        when(solicitudRepository.findAll(anySpecification(), any(Sort.class))).thenReturn(List.of(
                first, second,
                solicitud(3, EstadoSolicitudEnum.REGISTRADA),
                solicitud(4, EstadoSolicitudEnum.EN_ATENCION)));

        DashboardResponse response = dashboardService.build(null, null, null, null);

        assertThat(response.getPorcentajeResuelto()).isEqualTo(50.0);
        assertThat(response.getTiempoPromedioAtencionHoras()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("UT-DSH-05 agrupa por categoría, prioridad y responsable")
    void groupsByCategoryPriorityAndResponsible() {
        CategoriaEntity redes = categoria(1, "Redes");
        CategoriaEntity soporte = categoria(2, "Soporte");
        PrioridadEntity alta = prioridad(1, "Alta", 3);
        PrioridadEntity media = prioridad(2, "Media", 2);
        UsuarioEntity ana = tecnico(10, "Ana", "Perez");
        UsuarioEntity luis = tecnico(11, "Luis", "Gomez");

        SolicitudEntity first = solicitud(1, EstadoSolicitudEnum.REGISTRADA);
        first.setCategoria(redes);
        first.setPrioridad(alta);
        SolicitudEntity second = solicitud(2, EstadoSolicitudEnum.REGISTRADA);
        second.setCategoria(redes);
        second.setPrioridad(alta);
        SolicitudEntity third = solicitud(3, EstadoSolicitudEnum.REGISTRADA);
        third.setCategoria(soporte);
        third.setPrioridad(media);
        SolicitudEntity fourth = solicitud(4, EstadoSolicitudEnum.REGISTRADA);

        when(solicitudRepository.findAll(anySpecification(), any(Sort.class)))
                .thenReturn(List.of(first, second, third, fourth));
        when(asignacionRepository.findBySolicitudIdInAndFinalizadaEnIsNull(anyList())).thenReturn(List.of(
                asignacion(first, ana), asignacion(second, ana), asignacion(third, luis)));

        DashboardResponse response = dashboardService.build(null, null, null, null);

        assertThat(response.getPorCategoria())
                .extracting(IndicadorResponse::getNombre, IndicadorResponse::getTotal)
                .containsExactly(tuple("Redes", 2L), tuple("Soporte", 1L));
        assertThat(response.getPorPrioridad())
                .extracting(IndicadorResponse::getNombre, IndicadorResponse::getTotal)
                .containsExactly(tuple("Alta", 2L), tuple("Media", 1L));
        assertThat(response.getPorResponsable())
                .extracting(IndicadorResponse::getNombre, IndicadorResponse::getTotal)
                .containsExactly(tuple("Ana Perez", 2L), tuple("Luis Gomez", 1L));
    }

    private Specification<SolicitudEntity> anySpecification() {
        return any();
    }

    private SolicitudEntity solicitud(Integer id, EstadoSolicitudEnum estado) {
        SolicitudEntity solicitud = new SolicitudEntity();
        ReflectionTestUtils.setField(solicitud, "id", id);
        solicitud.setCodigo("SOL-2026-00000" + id);
        solicitud.setTitulo("Solicitud " + id);
        solicitud.setDescripcion("Descripción");
        solicitud.setUbicacion("Ubicación");
        solicitud.setEstado(estado);
        return solicitud;
    }

    private void creadoEn(SolicitudEntity solicitud, LocalDateTime date) {
        ReflectionTestUtils.setField(solicitud, "creadoEn", date);
    }

    private CategoriaEntity categoria(Integer id, String nombre) {
        CategoriaEntity categoria = new CategoriaEntity();
        ReflectionTestUtils.setField(categoria, "id", id);
        categoria.setCodigo("CAT-" + id);
        categoria.setNombre(nombre);
        return categoria;
    }

    private PrioridadEntity prioridad(Integer id, String nombre, int nivel) {
        PrioridadEntity prioridad = new PrioridadEntity();
        ReflectionTestUtils.setField(prioridad, "id", id);
        prioridad.setCodigo("PRI-" + id);
        prioridad.setNombre(nombre);
        prioridad.setNivel((byte) nivel);
        return prioridad;
    }

    private UsuarioEntity tecnico(Integer id, String nombres, String apellidos) {
        UsuarioEntity user = new UsuarioEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setNombres(nombres);
        user.setApellidos(apellidos);
        user.setCorreo(nombres.toLowerCase() + "@sigsu.local");
        user.setPasswordHash("$2a$10$hash");
        user.setRol(RolEnum.TECNICO);
        user.setActivo(true);
        return user;
    }

    private AsignacionEntity asignacion(SolicitudEntity solicitud, UsuarioEntity tecnico) {
        AsignacionEntity asignacion = new AsignacionEntity();
        asignacion.setSolicitud(solicitud);
        asignacion.setTecnico(tecnico);
        asignacion.setAsignadoPor(tecnico);
        asignacion.setAsignadaEn(LocalDateTime.now());
        return asignacion;
    }
}
