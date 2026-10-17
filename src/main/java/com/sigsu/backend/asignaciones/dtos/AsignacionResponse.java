package com.sigsu.backend.asignaciones.dtos;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;

import java.time.LocalDateTime;

public class AsignacionResponse {

    private final Integer id;
    private final Integer solicitudId;
    private final String solicitudCodigo;
    private final Integer tecnicoId;
    private final String tecnicoNombre;
    private final Integer asignadoPorId;
    private final String asignadoPorNombre;
    private final LocalDateTime asignadaEn;
    private final LocalDateTime finalizadaEn;

    public AsignacionResponse(Integer id, Integer solicitudId, String solicitudCodigo, Integer tecnicoId,
                              String tecnicoNombre, Integer asignadoPorId, String asignadoPorNombre,
                              LocalDateTime asignadaEn, LocalDateTime finalizadaEn) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.solicitudCodigo = solicitudCodigo;
        this.tecnicoId = tecnicoId;
        this.tecnicoNombre = tecnicoNombre;
        this.asignadoPorId = asignadoPorId;
        this.asignadoPorNombre = asignadoPorNombre;
        this.asignadaEn = asignadaEn;
        this.finalizadaEn = finalizadaEn;
    }

    public static AsignacionResponse from(AsignacionEntity asignacion) {
        return new AsignacionResponse(
                asignacion.getId(),
                asignacion.getSolicitud().getId(),
                asignacion.getSolicitud().getCodigo(),
                asignacion.getTecnico().getId(),
                asignacion.getTecnico().getNombres() + " " + asignacion.getTecnico().getApellidos(),
                asignacion.getAsignadoPor().getId(),
                asignacion.getAsignadoPor().getNombres() + " " + asignacion.getAsignadoPor().getApellidos(),
                asignacion.getAsignadaEn(),
                asignacion.getFinalizadaEn());
    }

    public Integer getId() {
        return id;
    }

    public Integer getSolicitudId() {
        return solicitudId;
    }

    public String getSolicitudCodigo() {
        return solicitudCodigo;
    }

    public Integer getTecnicoId() {
        return tecnicoId;
    }

    public String getTecnicoNombre() {
        return tecnicoNombre;
    }

    public Integer getAsignadoPorId() {
        return asignadoPorId;
    }

    public String getAsignadoPorNombre() {
        return asignadoPorNombre;
    }

    public LocalDateTime getAsignadaEn() {
        return asignadaEn;
    }

    public LocalDateTime getFinalizadaEn() {
        return finalizadaEn;
    }
}
