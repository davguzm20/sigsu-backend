package com.sigsu.backend.solicitudes.services;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import com.sigsu.backend.common.storage.CloudinaryStorage;
import com.sigsu.backend.solicitudes.dtos.EvidenciaResponse;
import com.sigsu.backend.solicitudes.entities.EvidenciaEntity;
import com.sigsu.backend.solicitudes.entities.SeguimientoEntity;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.EvidenciaRepository;
import com.sigsu.backend.solicitudes.repositories.SeguimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenciaService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp",
            "application/pdf");
    private static final String EMPTY_FILE = "El archivo de la evidencia es obligatorio";
    private static final String INVALID_EXTENSION = "Formato no permitido. Se aceptan jpg, jpeg, png, webp y pdf";
    private static final String INVALID_CONTENT_TYPE = "El tipo de archivo no corresponde a un formato permitido";
    private static final String SEGUIMIENTO_NOT_FOUND = "Seguimiento no encontrado";
    private static final String SEGUIMIENTO_OTRA_SOLICITUD = "El seguimiento no pertenece a la solicitud";

    private final SolicitudService solicitudService;
    private final SeguimientoRepository seguimientoRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final CloudinaryStorage cloudinaryStorage;

    public EvidenciaService(SolicitudService solicitudService, SeguimientoRepository seguimientoRepository,
                            EvidenciaRepository evidenciaRepository, CloudinaryStorage cloudinaryStorage) {
        this.solicitudService = solicitudService;
        this.seguimientoRepository = seguimientoRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.cloudinaryStorage = cloudinaryStorage;
    }

    @Transactional
    public EvidenciaResponse agregar(Integer solicitudId, MultipartFile archivo, Integer seguimientoId,
                                     UsuarioEntity user) {
        validate(archivo);

        SolicitudEntity solicitud = solicitudService.findVisible(solicitudId, user);
        SeguimientoEntity seguimiento = findSeguimiento(seguimientoId, solicitudId);

        String publicId = "solicitud_" + solicitudId + "_" + UUID.randomUUID().toString().replace("-", "");
        String archivoUrl = cloudinaryStorage.upload(archivo, publicId);

        EvidenciaEntity evidencia = new EvidenciaEntity();
        evidencia.setSolicitud(solicitud);
        evidencia.setSeguimiento(seguimiento);
        evidencia.setCargadoPor(user);
        evidencia.setNombreArchivo(archivo.getOriginalFilename());
        evidencia.setTipoMime(archivo.getContentType());
        evidencia.setTamanoBytes(archivo.getSize());
        evidencia.setArchivoUrl(archivoUrl);

        return EvidenciaResponse.from(evidenciaRepository.save(evidencia));
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listar(Integer solicitudId, UsuarioEntity user) {
        solicitudService.findVisible(solicitudId, user);

        return evidenciaRepository.findBySolicitudIdOrderByCreadoEnAsc(solicitudId).stream()
                .map(EvidenciaResponse::from)
                .toList();
    }

    private SeguimientoEntity findSeguimiento(Integer seguimientoId, Integer solicitudId) {
        if (seguimientoId == null) {
            return null;
        }

        SeguimientoEntity seguimiento = seguimientoRepository.findById(seguimientoId)
                .orElseThrow(() -> new ResourceNotFoundException(SEGUIMIENTO_NOT_FOUND));

        if (!seguimiento.getSolicitud().getId().equals(solicitudId)) {
            throw new BusinessRuleException(SEGUIMIENTO_OTRA_SOLICITUD);
        }

        return seguimiento;
    }

    private void validate(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException(EMPTY_FILE);
        }

        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessRuleException(INVALID_EXTENSION);
        }
        if (archivo.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(archivo.getContentType().toLowerCase())) {
            throw new BusinessRuleException(INVALID_CONTENT_TYPE);
        }
    }
}
