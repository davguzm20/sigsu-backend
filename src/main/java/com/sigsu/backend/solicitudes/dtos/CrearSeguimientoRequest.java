package com.sigsu.backend.solicitudes.dtos;

import jakarta.validation.constraints.NotBlank;

public class CrearSeguimientoRequest {

    @NotBlank(message = "El contenido del comentario es obligatorio")
    private String contenido;

    public CrearSeguimientoRequest() {
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }
}
