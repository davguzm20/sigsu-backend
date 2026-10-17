package com.sigsu.backend.asignaciones.services;

import com.sigsu.backend.asignaciones.dtos.AsignacionResponse;
import com.sigsu.backend.asignaciones.dtos.AsignarTecnicoRequest;
import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.exceptions.ConflictException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AsignacionService {

    private static final String SOLICITUD_NOT_FOUND = "Solicitud no encontrada";
    private static final String TECNICO_NOT_FOUND = "Técnico no encontrado";
    private static final String NEEDS_CLASSIFICATION = "La solicitud debe tener categoría y área antes de asignar un técnico";
    private static final String ALREADY_ASSIGNED = "La solicitud ya tiene un técnico asignado";
    private static final String NOT_A_TECNICO = "El usuario indicado no tiene el rol de técnico";
    private static final String INACTIVE_TECNICO = "El técnico indicado está desactivado";

    private final SolicitudRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final AsignacionRepository asignacionRepository;

    public AsignacionService(SolicitudRepository solicitudRepository, UsuarioRepository usuarioRepository,
                             AsignacionRepository asignacionRepository) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.asignacionRepository = asignacionRepository;
    }

    @Transactional
    public AsignacionResponse asignar(Integer solicitudId, AsignarTecnicoRequest request, UsuarioEntity user) {
        SolicitudEntity solicitud = solicitudRepository.findByIdForUpdate(solicitudId)
                .orElseThrow(() -> new ResourceNotFoundException(SOLICITUD_NOT_FOUND));

        if (solicitud.getCategoria() == null || solicitud.getArea() == null) {
            throw new BusinessRuleException(NEEDS_CLASSIFICATION);
        }
        if (asignacionRepository.existsBySolicitudIdAndFinalizadaEnIsNull(solicitudId)) {
            throw new ConflictException(ALREADY_ASSIGNED);
        }

        UsuarioEntity tecnico = usuarioRepository.findById(request.getTecnicoId())
                .orElseThrow(() -> new ResourceNotFoundException(TECNICO_NOT_FOUND));

        if (tecnico.getRol() != RolEnum.TECNICO) {
            throw new BusinessRuleException(NOT_A_TECNICO);
        }
        if (!Boolean.TRUE.equals(tecnico.getActivo())) {
            throw new BusinessRuleException(INACTIVE_TECNICO);
        }

        AsignacionEntity asignacion = new AsignacionEntity();
        asignacion.setSolicitud(solicitud);
        asignacion.setTecnico(tecnico);
        asignacion.setAsignadoPor(user);
        asignacion.setAsignadaEn(LocalDateTime.now());

        return AsignacionResponse.from(asignacionRepository.save(asignacion));
    }
}
