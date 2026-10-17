package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.solicitudes.entities.EvidenciaEntity;

import java.time.LocalDateTime;

public class EvidenciaResponse {

    private final Integer id;
    private final Integer solicitudId;
    private final Integer seguimientoId;
    private final String nombreArchivo;
    private final String tipoMime;
    private final Long tamanoBytes;
    private final String archivoUrl;
    private final Integer cargadoPorId;
    private final String cargadoPorNombre;
    private final LocalDateTime creadoEn;

    public EvidenciaResponse(Integer id, Integer solicitudId, Integer seguimientoId, String nombreArchivo,
                             String tipoMime, Long tamanoBytes, String archivoUrl, Integer cargadoPorId,
                             String cargadoPorNombre, LocalDateTime creadoEn) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.seguimientoId = seguimientoId;
        this.nombreArchivo = nombreArchivo;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamanoBytes;
        this.archivoUrl = archivoUrl;
        this.cargadoPorId = cargadoPorId;
        this.cargadoPorNombre = cargadoPorNombre;
        this.creadoEn = creadoEn;
    }

    public static EvidenciaResponse from(EvidenciaEntity evidencia) {
        return new EvidenciaResponse(
                evidencia.getId(),
                evidencia.getSolicitud().getId(),
                evidencia.getSeguimiento() == null ? null : evidencia.getSeguimiento().getId(),
                evidencia.getNombreArchivo(),
                evidencia.getTipoMime(),
                evidencia.getTamanoBytes(),
                evidencia.getArchivoUrl(),
                evidencia.getCargadoPor().getId(),
                evidencia.getCargadoPor().getNombres() + " " + evidencia.getCargadoPor().getApellidos(),
                evidencia.getCreadoEn());
    }

    public Integer getId() {
        return id;
    }

    public Integer getSolicitudId() {
        return solicitudId;
    }

    public Integer getSeguimientoId() {
        return seguimientoId;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public String getArchivoUrl() {
        return archivoUrl;
    }

    public Integer getCargadoPorId() {
        return cargadoPorId;
    }

    public String getCargadoPorNombre() {
        return cargadoPorNombre;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
