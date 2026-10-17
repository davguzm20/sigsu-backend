package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.auth.entities.UsuarioEntity;

public class UsuarioResumenResponse {

    private final Integer id;
    private final String nombres;
    private final String apellidos;

    public UsuarioResumenResponse(Integer id, String nombres, String apellidos) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
    }

    public static UsuarioResumenResponse from(UsuarioEntity user) {
        return new UsuarioResumenResponse(user.getId(), user.getNombres(), user.getApellidos());
    }

    public Integer getId() {
        return id;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }
}
