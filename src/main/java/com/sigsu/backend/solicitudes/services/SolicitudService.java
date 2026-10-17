package com.sigsu.backend.solicitudes.services;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.solicitudes.dtos.CrearSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.SolicitudResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class SolicitudService {

    private static final String CODE_PREFIX = "SOL-";
    private static final String CODE_SEQUENCE_FORMAT = "%06d";

    private final SolicitudRepository solicitudRepository;

    public SolicitudService(SolicitudRepository solicitudRepository) {
        this.solicitudRepository = solicitudRepository;
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
