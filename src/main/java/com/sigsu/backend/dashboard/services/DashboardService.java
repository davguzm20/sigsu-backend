package com.sigsu.backend.dashboard.services;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.dashboard.dtos.DashboardResponse;
import com.sigsu.backend.dashboard.dtos.IndicadorResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class DashboardService {

    private static final Set<EstadoSolicitudEnum> ATENDIDAS = Set.of(
            EstadoSolicitudEnum.EN_ATENCION, EstadoSolicitudEnum.RESUELTA, EstadoSolicitudEnum.CERRADA);
    private static final Set<EstadoSolicitudEnum> RESUELTAS = Set.of(
            EstadoSolicitudEnum.RESUELTA, EstadoSolicitudEnum.CERRADA);
    private static final Sort CREATED_DESC = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final SolicitudRepository solicitudRepository;
    private final AsignacionRepository asignacionRepository;

    public DashboardService(SolicitudRepository solicitudRepository, AsignacionRepository asignacionRepository) {
        this.solicitudRepository = solicitudRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse build(LocalDate desde, LocalDate hasta, Integer categoriaId, Integer prioridadId) {
        List<SolicitudEntity> solicitudes = solicitudRepository.findAll(
                buildSpecification(desde, hasta, categoriaId, prioridadId), CREATED_DESC);
        Map<Integer, AsignacionEntity> responsables = loadActiveAssignments(solicitudes);

        long total = solicitudes.size();
        long pendientes = solicitudes.stream()
                .filter(solicitud -> solicitud.getEstado() != EstadoSolicitudEnum.CERRADA)
                .count();
        long atendidas = solicitudes.stream()
                .filter(solicitud -> ATENDIDAS.contains(solicitud.getEstado()))
                .count();
        long resueltas = solicitudes.stream()
                .filter(solicitud -> RESUELTAS.contains(solicitud.getEstado()))
                .count();
        long vencidas = solicitudes.stream()
                .filter(this::isOverdue)
                .count();

        double porcentajeResuelto = total == 0 ? 0 : redondear(resueltas * 100.0 / total);
        double tiempoPromedio = redondear(averageMinutes(solicitudes) / 60.0);

        return new DashboardResponse(total, pendientes, atendidas, vencidas, porcentajeResuelto, tiempoPromedio,
                agruparPorCategoria(solicitudes), agruparPorPrioridad(solicitudes),
                agruparPorResponsable(solicitudes, responsables));
    }

    private boolean isOverdue(SolicitudEntity solicitud) {
        return solicitud.getFechaLimite() != null
                && solicitud.getFechaLimite().isBefore(LocalDateTime.now())
                && solicitud.getEstado() != EstadoSolicitudEnum.CERRADA;
    }

    private double averageMinutes(List<SolicitudEntity> solicitudes) {
        return solicitudes.stream()
                .filter(solicitud -> solicitud.getResueltaEn() != null)
                .mapToLong(solicitud -> Duration.between(solicitud.getCreadoEn(), solicitud.getResueltaEn()).toMinutes())
                .average()
                .orElse(0);
    }

    private double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private List<IndicadorResponse> agruparPorCategoria(List<SolicitudEntity> solicitudes) {
        return agrupar(solicitudes, solicitud -> solicitud.getCategoria() == null ? null
                : new IndicadorResponse(solicitud.getCategoria().getId(), solicitud.getCategoria().getNombre(), 0L));
    }

    private List<IndicadorResponse> agruparPorPrioridad(List<SolicitudEntity> solicitudes) {
        return agrupar(solicitudes, solicitud -> solicitud.getPrioridad() == null ? null
                : new IndicadorResponse(solicitud.getPrioridad().getId(), solicitud.getPrioridad().getNombre(), 0L));
    }

    private List<IndicadorResponse> agruparPorResponsable(List<SolicitudEntity> solicitudes,
                                                          Map<Integer, AsignacionEntity> responsables) {
        return agrupar(solicitudes, solicitud -> {
            AsignacionEntity asignacion = responsables.get(solicitud.getId());

            return asignacion == null ? null
                    : new IndicadorResponse(asignacion.getTecnico().getId(),
                            asignacion.getTecnico().getNombres() + " " + asignacion.getTecnico().getApellidos(), 0L);
        });
    }

    private List<IndicadorResponse> agrupar(List<SolicitudEntity> solicitudes,
                                            Function<SolicitudEntity, IndicadorResponse> clave) {
        Map<Integer, String> nombres = new HashMap<>();
        Map<Integer, Long> conteos = new HashMap<>();

        for (SolicitudEntity solicitud : solicitudes) {
            IndicadorResponse indicador = clave.apply(solicitud);
            if (indicador == null) {
                continue;
            }

            nombres.put(indicador.getId(), indicador.getNombre());
            conteos.merge(indicador.getId(), 1L, Long::sum);
        }

        return conteos.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .map(entry -> new IndicadorResponse(entry.getKey(), nombres.get(entry.getKey()), entry.getValue()))
                .toList();
    }

    private Map<Integer, AsignacionEntity> loadActiveAssignments(List<SolicitudEntity> solicitudes) {
        if (solicitudes.isEmpty()) {
            return Map.of();
        }

        List<Integer> ids = solicitudes.stream().map(SolicitudEntity::getId).toList();

        return asignacionRepository.findBySolicitudIdInAndFinalizadaEnIsNull(ids).stream()
                .collect(java.util.stream.Collectors.toMap(asignacion -> asignacion.getSolicitud().getId(),
                        Function.identity()));
    }

    private Specification<SolicitudEntity> buildSpecification(LocalDate desde, LocalDate hasta, Integer categoriaId,
                                                              Integer prioridadId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("creadoEn"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("creadoEn"), hasta.atTime(LocalTime.MAX)));
            }
            if (categoriaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoria").get("id"), categoriaId));
            }
            if (prioridadId != null) {
                predicates.add(criteriaBuilder.equal(root.get("prioridad").get("id"), prioridadId));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
