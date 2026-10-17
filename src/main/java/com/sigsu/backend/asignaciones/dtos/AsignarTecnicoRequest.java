package com.sigsu.backend.asignaciones.dtos;

import jakarta.validation.constraints.NotNull;

public class AsignarTecnicoRequest {

    @NotNull(message = "El técnico es obligatorio")
    private Integer tecnicoId;

    public AsignarTecnicoRequest() {
    }

    public Integer getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Integer tecnicoId) {
        this.tecnicoId = tecnicoId;
    }
}
