package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.solicitudes.entities.SeguimientoEntity;

import java.time.LocalDateTime;

public class SeguimientoResponse {

    private final Integer id;
    private final String tipo;
    private final String contenido;
    private final Integer creadoPorId;
    private final String creadoPorNombre;
    private final LocalDateTime creadoEn;

    public SeguimientoResponse(Integer id, String tipo, String contenido, Integer creadoPorId, String creadoPorNombre,
                               LocalDateTime creadoEn) {
        this.id = id;
        this.tipo = tipo;
        this.contenido = contenido;
        this.creadoPorId = creadoPorId;
        this.creadoPorNombre = creadoPorNombre;
        this.creadoEn = creadoEn;
    }

    public static SeguimientoResponse from(SeguimientoEntity seguimiento) {
        return new SeguimientoResponse(
                seguimiento.getId(),
                seguimiento.getTipo().name(),
                seguimiento.getContenido(),
                seguimiento.getCreadoPor().getId(),
                seguimiento.getCreadoPor().getNombres() + " " + seguimiento.getCreadoPor().getApellidos(),
                seguimiento.getCreadoEn());
    }

    public Integer getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getContenido() {
        return contenido;
    }

    public Integer getCreadoPorId() {
        return creadoPorId;
    }

    public String getCreadoPorNombre() {
        return creadoPorNombre;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
