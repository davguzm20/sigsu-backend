package com.sigsu.backend.solicitudes.services;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.catalogos.repositories.CategoriaRepository;
import com.sigsu.backend.catalogos.repositories.PrioridadRepository;
import com.sigsu.backend.common.exceptions.ForbiddenException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import com.sigsu.backend.solicitudes.dtos.ClasificarSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.CrearSeguimientoRequest;
import com.sigsu.backend.solicitudes.dtos.CrearSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.SeguimientoResponse;
import com.sigsu.backend.solicitudes.dtos.SolicitudResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SeguimientoEntity;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.entities.TipoSeguimientoEnum;
import com.sigsu.backend.solicitudes.repositories.SeguimientoRepository;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

    private static final String CODE_PREFIX = "SOL-";
    private static final String CODE_SEQUENCE_FORMAT = "%06d";
    private static final String SOLICITUD_NOT_FOUND = "Solicitud no encontrada";
    private static final String FORBIDDEN_SOLICITUD = "No tiene permiso para consultar esta solicitud";
    private static final String CATEGORIA_NOT_FOUND = "Categoría no encontrada";
    private static final String PRIORIDAD_NOT_FOUND = "Prioridad no encontrada";
    private static final String AREA_NOT_FOUND = "Área no encontrada";
    private static final Sort CREATED_DESC = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final SolicitudRepository solicitudRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final AsignacionRepository asignacionRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;
    private final AreaRepository areaRepository;

    public SolicitudService(SolicitudRepository solicitudRepository, SeguimientoRepository seguimientoRepository,
                            AsignacionRepository asignacionRepository, CategoriaRepository categoriaRepository,
                            PrioridadRepository prioridadRepository, AreaRepository areaRepository) {
        this.solicitudRepository = solicitudRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.asignacionRepository = asignacionRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
        this.areaRepository = areaRepository;
    }

    @Transactional
    public SolicitudResponse register(CrearSolicitudRequest request, UsuarioEntity user) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setCodigo(generateCode());
        solicitud.setSolicitante(user);
        solicitud.setEstado(EstadoSolicitudEnum.REGISTRADA);
        solicitud.setTitulo(request.getTitulo());
        solicitud.setDescripcion(request.getDescripcion());
        solicitud.setUbicacion(request.getUbicacion());

        return SolicitudResponse.from(solicitudRepository.save(solicitud), null);
    }

    @Transactional
    public SolicitudResponse clasificar(Integer solicitudId, ClasificarSolicitudRequest request) {
        SolicitudEntity solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));

        CategoriaEntity categoria = categoriaRepository.findByIdAndActivoTrue(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORIA_NOT_FOUND));
        PrioridadEntity prioridad = prioridadRepository.findByIdAndActivoTrue(request.getPrioridadId())
                .orElseThrow(() -> new ResourceNotFoundException(PRIORIDAD_NOT_FOUND));
        AreaEntity area = areaRepository.findByIdAndActivoTrue(request.getAreaId())
                .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));

        solicitud.setCategoria(categoria);
        solicitud.setPrioridad(prioridad);
        solicitud.setArea(area);

        return SolicitudResponse.from(solicitud, activeAssignment(solicitud.getId()));
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> search(UsuarioEntity user, EstadoSolicitudEnum estado, String texto, LocalDate desde,
                                          LocalDate hasta, Integer categoriaId, Integer prioridadId,
                                          Integer responsableId, Boolean sinAsignar) {
        Specification<SolicitudEntity> specification = buildSpecification(user, estado, texto, desde, hasta,
                categoriaId, prioridadId, responsableId, sinAsignar);
        List<SolicitudEntity> solicitudes = solicitudRepository.findAll(specification, CREATED_DESC);
        Map<Integer, AsignacionEntity> responsables = loadActiveAssignments(solicitudes);

        return solicitudes.stream()
                .map(solicitud -> SolicitudResponse.from(solicitud, responsables.get(solicitud.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitudResponse findDetail(Integer solicitudId, UsuarioEntity user) {
        SolicitudEntity solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));
        checkAccess(solicitud, user);

        return SolicitudResponse.from(solicitud, activeAssignment(solicitud.getId()));
    }

    @Transactional(readOnly = true)
    public SolicitudResponse findDetailByCodigo(String codigo, UsuarioEntity user) {
        SolicitudEntity solicitud = solicitudRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));
        checkAccess(solicitud, user);

        return SolicitudResponse.from(solicitud, activeAssignment(solicitud.getId()));
    }

    @Transactional
    public SeguimientoResponse addSeguimiento(Integer solicitudId, CrearSeguimientoRequest request, UsuarioEntity user) {
        SolicitudEntity solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));
        checkAccess(solicitud, user);

        SeguimientoEntity seguimiento = new SeguimientoEntity();
        seguimiento.setSolicitud(solicitud);
        seguimiento.setCreadoPor(user);
        seguimiento.setTipo(TipoSeguimientoEnum.COMENTARIO);
        seguimiento.setContenido(request.getContenido());

        return SeguimientoResponse.from(seguimientoRepository.save(seguimiento));
    }

    private AsignacionEntity activeAssignment(Integer solicitudId) {
        return asignacionRepository.findFirstBySolicitudIdAndFinalizadaEnIsNull(solicitudId).orElse(null);
    }

    private Map<Integer, AsignacionEntity> loadActiveAssignments(List<SolicitudEntity> solicitudes) {
        if (solicitudes.isEmpty()) {
            return Map.of();
        }

        List<Integer> ids = solicitudes.stream().map(SolicitudEntity::getId).toList();

        return asignacionRepository.findBySolicitudIdInAndFinalizadaEnIsNull(ids).stream()
                .collect(Collectors.toMap(asignacion -> asignacion.getSolicitud().getId(), Function.identity()));
    }

    private void checkAccess(SolicitudEntity solicitud, UsuarioEntity user) {
        if (!canView(solicitud, user)) {
            throw new ForbiddenException(FORBIDDEN_SOLICITUD);
        }
    }

    private boolean canView(SolicitudEntity solicitud, UsuarioEntity user) {
        return switch (user.getRol()) {
            case SOLICITANTE -> solicitud.getSolicitante().getId().equals(user.getId());
            case TECNICO -> asignacionRepository.existsBySolicitudIdAndTecnicoIdAndFinalizadaEnIsNull(
                    solicitud.getId(), user.getId());
            case SUPERVISOR, ADMINISTRADOR -> true;
        };
    }

    private Specification<SolicitudEntity> buildSpecification(UsuarioEntity user, EstadoSolicitudEnum estado, String texto,
                                                              LocalDate desde, LocalDate hasta, Integer categoriaId,
                                                              Integer prioridadId, Integer responsableId,
                                                              Boolean sinAsignar) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(buildScope(user, root, criteriaBuilder, query));

            if (estado != null) {
                predicates.add(criteriaBuilder.equal(root.get("estado"), estado));
            }
            if (texto != null && !texto.isBlank()) {
                predicates.add(buildTextFilter(texto, root, criteriaBuilder));
            }
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
            if (responsableId != null) {
                predicates.add(criteriaBuilder.exists(buildActiveAssignment(query, criteriaBuilder, root, responsableId)));
            }
            if (Boolean.TRUE.equals(sinAsignar)) {
                predicates.add(criteriaBuilder.not(criteriaBuilder.exists(
                        buildActiveAssignment(query, criteriaBuilder, root, null))));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildScope(UsuarioEntity user, Root<SolicitudEntity> root, CriteriaBuilder criteriaBuilder,
                                 CriteriaQuery<?> query) {
        return switch (user.getRol()) {
            case SOLICITANTE -> criteriaBuilder.equal(root.get("solicitante").get("id"), user.getId());
            case TECNICO -> criteriaBuilder.exists(buildActiveAssignment(query, criteriaBuilder, root, user.getId()));
            case SUPERVISOR, ADMINISTRADOR -> criteriaBuilder.conjunction();
        };
    }

    private Subquery<Integer> buildActiveAssignment(CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder,
                                                    Root<SolicitudEntity> solicitudRoot, Integer tecnicoId) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<AsignacionEntity> asignacion = subquery.from(AsignacionEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(asignacion.get("solicitud").get("id"), solicitudRoot.get("id")));
        predicates.add(criteriaBuilder.isNull(asignacion.get("finalizadaEn")));

        if (tecnicoId != null) {
            predicates.add(criteriaBuilder.equal(asignacion.get("tecnico").get("id"), tecnicoId));
        }

        subquery.select(criteriaBuilder.literal(1));
        subquery.where(predicates.toArray(new Predicate[0]));

        return subquery;
    }

    private Predicate buildTextFilter(String texto, Root<SolicitudEntity> root, CriteriaBuilder criteriaBuilder) {
        String pattern = "%" + texto.trim().toLowerCase() + "%";

        return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("codigo")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("titulo")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), pattern));
    }

    private String generateCode() {
        String prefix = CODE_PREFIX + LocalDate.now().getYear() + "-";
        int nextSequence = solicitudRepository.findFirstByCodigoStartingWithOrderByCodigoDesc(prefix)
                .map(last -> parseSequence(last.getCodigo(), prefix) + 1)
                .orElse(1);

        return prefix + String.format(CODE_SEQUENCE_FORMAT, nextSequence);
    }

    private int parseSequence(String codigo, String prefix) {
        return Integer.parseInt(codigo.substring(prefix.length()));
    }
}
