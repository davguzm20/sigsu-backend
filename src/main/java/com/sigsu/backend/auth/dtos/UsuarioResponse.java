package com.sigsu.backend.auth.dtos;

import com.sigsu.backend.auth.entities.UsuarioEntity;

public class UsuarioResponse {

    private final Integer id;
    private final String nombres;
    private final String apellidos;
    private final String correo;
    private final String rol;

    public UsuarioResponse(Integer id, String nombres, String apellidos, String correo, String rol) {
        this.id = id;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.correo = correo;
        this.rol = rol;
    }

    public static UsuarioResponse from(UsuarioEntity user) {
        return new UsuarioResponse(
                user.getId(),
                user.getNombres(),
                user.getApellidos(),
                user.getCorreo(),
                user.getRol().name());
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

    public String getCorreo() {
        return correo;
    }

    public String getRol() {
        return rol;
    }
}
