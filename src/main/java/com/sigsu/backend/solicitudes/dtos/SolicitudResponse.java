package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;

import java.time.LocalDateTime;

public class SolicitudResponse {

    private final Integer id;
    private final String codigo;
    private final String estado;
    private final String titulo;
    private final String descripcion;
    private final String ubicacion;
    private final Integer solicitanteId;
    private final String solicitanteNombre;
    private final Integer categoriaId;
    private final String categoriaNombre;
    private final Integer prioridadId;
    private final String prioridadNombre;
    private final Integer areaId;
    private final String areaNombre;
    private final Integer responsableId;
    private final String responsableNombre;
    private final String solucion;
    private final LocalDateTime resueltaEn;
    private final LocalDateTime cerradaEn;
    private final LocalDateTime creadoEn;
    private final LocalDateTime actualizadoEn;

    public SolicitudResponse(Integer id, String codigo, String estado, String titulo, String descripcion, String ubicacion,
                             Integer solicitanteId, String solicitanteNombre, Integer categoriaId, String categoriaNombre,
                             Integer prioridadId, String prioridadNombre, Integer areaId, String areaNombre,
                             Integer responsableId, String responsableNombre, String solucion, LocalDateTime resueltaEn,
                             LocalDateTime cerradaEn, LocalDateTime creadoEn, LocalDateTime actualizadoEn) {
        this.id = id;
        this.codigo = codigo;
        this.estado = estado;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.ubicacion = ubicacion;
        this.solicitanteId = solicitanteId;
        this.solicitanteNombre = solicitanteNombre;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.prioridadId = prioridadId;
        this.prioridadNombre = prioridadNombre;
        this.areaId = areaId;
        this.areaNombre = areaNombre;
        this.responsableId = responsableId;
        this.responsableNombre = responsableNombre;
        this.solucion = solucion;
        this.resueltaEn = resueltaEn;
        this.cerradaEn = cerradaEn;
        this.creadoEn = creadoEn;
        this.actualizadoEn = actualizadoEn;
    }

    public static SolicitudResponse from(SolicitudEntity solicitud, AsignacionEntity asignacion) {
        CategoriaEntity categoria = solicitud.getCategoria();
        PrioridadEntity prioridad = solicitud.getPrioridad();
        AreaEntity area = solicitud.getArea();

        return new SolicitudResponse(
                solicitud.getId(),
                solicitud.getCodigo(),
                solicitud.getEstado().name(),
                solicitud.getTitulo(),
                solicitud.getDescripcion(),
                solicitud.getUbicacion(),
                solicitud.getSolicitante().getId(),
                solicitud.getSolicitante().getNombres() + " " + solicitud.getSolicitante().getApellidos(),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNombre(),
                prioridad == null ? null : prioridad.getId(),
                prioridad == null ? null : prioridad.getNombre(),
                area == null ? null : area.getId(),
                area == null ? null : area.getNombre(),
                asignacion == null ? null : asignacion.getTecnico().getId(),
                asignacion == null ? null : asignacion.getTecnico().getNombres() + " " + asignacion.getTecnico().getApellidos(),
                solicitud.getSolucion(),
                solicitud.getResueltaEn(),
                solicitud.getCerradaEn(),
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

    public Integer getSolicitanteId() {
        return solicitanteId;
    }

    public String getSolicitanteNombre() {
        return solicitanteNombre;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public Integer getPrioridadId() {
        return prioridadId;
    }

    public String getPrioridadNombre() {
        return prioridadNombre;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public String getAreaNombre() {
        return areaNombre;
    }

    public Integer getResponsableId() {
        return responsableId;
    }

    public String getResponsableNombre() {
        return responsableNombre;
    }

    public String getSolucion() {
        return solucion;
    }

    public LocalDateTime getResueltaEn() {
        return resueltaEn;
    }

    public LocalDateTime getCerradaEn() {
        return cerradaEn;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
