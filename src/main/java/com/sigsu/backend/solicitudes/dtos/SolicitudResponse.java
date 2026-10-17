package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.solicitudes.entities.SolicitudEntity;

import java.time.LocalDateTime;

public class SolicitudResponse {

    private final Integer id;
    private final String codigo;
    private final String estado;
    private final String titulo;
    private final String descripcion;
    private final String ubicacion;
    private final UsuarioResumenResponse solicitante;
    private final LocalDateTime creadoEn;
    private final LocalDateTime actualizadoEn;

    public SolicitudResponse(Integer id, String codigo, String estado, String titulo, String descripcion,
                            String ubicacion, UsuarioResumenResponse solicitante, LocalDateTime creadoEn,
                            LocalDateTime actualizadoEn) {
        this.id = id;
        this.codigo = codigo;
        this.estado = estado;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.solicitante = solicitante;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public static SolicitudResponse from(SolicitudEntity solicitud) {
        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getCodigo(),
                solicitud.getEstado().name(),
                solicitud.getTitulo(),
                solicitud.getDescripcion(),
                solicitud.getUbicacion(),
                UsuarioResumenResponse.from(solicitud.getSolicitante()),
                solicitud.getCreadoEn(),
                solicitud.getActualizadoEn());
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getEstado() {
        return estado;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public UsuarioResumenResponse getSolicitante() {
        return solicitante;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
