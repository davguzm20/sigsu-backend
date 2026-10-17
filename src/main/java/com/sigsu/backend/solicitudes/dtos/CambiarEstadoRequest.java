package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CambiarEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoSolicitudEnum estado;

    @Size(max = 250, message = "La observación no puede superar los 250 caracteres")
    private String observacion;

    private String solucion;

    public CambiarEstadoRequest() {
    }

    public EstadoSolicitudEnum getEstado() {
        return estado;
    }

    public void setEstado(EstadoSolicitudEnum estado) {
        this.estado = estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getSolucion() {
        return solucion;
    }

    public void setSolucion(String solucion) {
        this.solucion = solucion;
    }
}
