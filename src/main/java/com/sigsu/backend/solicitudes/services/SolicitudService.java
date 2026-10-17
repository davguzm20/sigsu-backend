package com.sigsu.backend.solicitudes.services;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.common.exceptions.ForbiddenException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
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
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SolicitudService {

    private static final String CODE_PREFIX = "SOL-";
    private static final String CODE_SEQUENCE_FORMAT = "%06d";
    private static final String SOLICITUD_NOT_FOUND = "Solicitud no encontrada";
    private static final String FORBIDDEN_SOLICITUD = "No tiene permiso para consultar esta solicitud";
    private static final Sort CREATED_DESC = Sort.by(Sort.Direction.DESC, "creadoEn");

    private final SolicitudRepository solicitudRepository;
    private final SeguimientoRepository seguimientoRepository;

    public SolicitudService(SolicitudRepository solicitudRepository, SeguimientoRepository seguimientoRepository) {
        this.solicitudRepository = solicitudRepository;
        this.seguimientoRepository = seguimientoRepository;
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

        return SolicitudResponse.from(solicitudRepository.save(solicitud));
    }

    @Transactional(readOnly = true)
    public List<SolicitudResponse> search(UsuarioEntity user, EstadoSolicitudEnum estado, String texto, LocalDate desde,
                                          LocalDate hasta) {
        Specification<SolicitudEntity> specification = buildSpecification(user, estado, texto, desde, hasta);

        return solicitudRepository.findAll(specification, CREATED_DESC).stream()
                .map(SolicitudResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitudResponse findDetail(Integer solicitudId, UsuarioEntity user) {
        SolicitudEntity solicitud = solicitudRepository.findById(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));
        checkAccess(solicitud, user);

        return SolicitudResponse.from(solicitud);
    }

    @Transactional(readOnly = true)
    public SolicitudResponse findDetailByCodigo(String codigo, UsuarioEntity user) {
        SolicitudEntity solicitud = solicitudRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));
        checkAccess(solicitud, user);

        return SolicitudResponse.from(solicitud);
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

    private void checkAccess(SolicitudEntity solicitud, UsuarioEntity user) {
        if (!canView(solicitud, user)) {
            throw new ForbiddenException(FORBIDDEN_SOLICITUD);
        }
    }

    private boolean canView(SolicitudEntity solicitud, UsuarioEntity user) {
        return switch (user.getRol()) {
            case SOLICITANTE -> solicitud.getSolicitante().getId().equals(user.getId());
            case TECNICO -> false;
            case SUPERVISOR, ADMINISTRADOR -> true;
        };
    }

    private Specification<SolicitudEntity> buildSpecification(UsuarioEntity user, EstadoSolicitudEnum estado, String texto,
                                                              LocalDate desde, LocalDate hasta) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(buildScope(user, root, criteriaBuilder));

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

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildScope(UsuarioEntity user, Root<SolicitudEntity> root, CriteriaBuilder criteriaBuilder) {
        return switch (user.getRol()) {
            case SOLICITANTE -> criteriaBuilder.equal(root.get("solicitante").get("id"), user.getId());
            case TECNICO -> criteriaBuilder.disjunction();
            case SUPERVISOR, ADMINISTRADOR -> criteriaBuilder.conjunction();
        };
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
