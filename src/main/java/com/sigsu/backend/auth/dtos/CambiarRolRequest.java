package com.sigsu.backend.auth.dtos;

import com.sigsu.backend.auth.entities.RolEnum;
import jakarta.validation.constraints.NotNull;

public class CambiarRolRequest {

    @NotNull(message = "El rol es obligatorio")
    private RolEnum rol;

    public CambiarRolRequest() {
    }

    public CambiarRolRequest(RolEnum rol) {
        this.rol = rol;
    }

    public RolEnum getRol() {
        return rol;
    }

    public void setRol(RolEnum rol) {
        this.rol = rol;
    }
}
