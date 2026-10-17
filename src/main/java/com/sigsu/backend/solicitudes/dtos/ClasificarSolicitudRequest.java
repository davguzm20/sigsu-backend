package com.sigsu.backend.solicitudes.dtos;

import jakarta.validation.constraints.NotNull;

public class ClasificarSolicitudRequest {

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoriaId;

    @NotNull(message = "La prioridad es obligatoria")
    private Integer prioridadId;

    @NotNull(message = "El área responsable es obligatoria")
    private Integer areaId;

    public ClasificarSolicitudRequest() {
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Integer getPrioridadId() {
        return prioridadId;
    }

    public void setPrioridadId(Integer prioridadId) {
        this.prioridadId = prioridadId;
    }

    public Integer getAreaId() {
        return areaId;
    }

    public void setAreaId(Integer areaId) {
        this.areaId = areaId;
    }
}
