package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.solicitudes.entities.HistorialEstadoEntity;

import java.time.LocalDateTime;

public class HistorialEstadoResponse {

    private final Integer id;
    private final String estadoOrigen;
    private final String estadoDestino;
    private final Integer cambiadoPorId;
    private final String cambiadoPorNombre;
    private final LocalDateTime cambiadoEn;
    private final String observacion;

    public HistorialEstadoResponse(Integer id, String estadoOrigen, String estadoDestino, Integer cambiadoPorId,
                                   String cambiadoPorNombre, LocalDateTime cambiadoEn, String observacion) {
        this.id = id;
        this.estadoOrigen = estadoOrigen;
        this.estadoDestino = estadoDestino;
        this.cambiadoPorId = cambiadoPorId;
        this.cambiadoPorNombre = cambiadoPorNombre;
        this.cambiadoEn = cambiadoEn;
        this.observacion = observacion;
    }

    public static HistorialEstadoResponse from(HistorialEstadoEntity historial) {
        return new HistorialEstadoResponse(
                historial.getId(),
                historial.getEstadoOrigen() == null ? null : historial.getEstadoOrigen().name(),
                historial.getEstadoDestino().name(),
                historial.getCambiadoPor().getId(),
                historial.getCambiadoPor().getNombres() + " " + historial.getCambiadoPor().getApellidos(),
                historial.getCambiadoEn(),
                historial.getObservacion());
    }

    public Integer getId() {
        return id;
    }

    public String getEstadoOrigen() {
        return estadoOrigen;
    }

    public String getEstadoDestino() {
        return estadoDestino;
    }

    public Integer getCambiadoPorId() {
        return cambiadoPorId;
    }

    public String getCambiadoPorNombre() {
        return cambiadoPorNombre;
    }

    public LocalDateTime getCambiadoEn() {
        return cambiadoEn;
    }

    public String getObservacion() {
        return observacion;
    }
}
