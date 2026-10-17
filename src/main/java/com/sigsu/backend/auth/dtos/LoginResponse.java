package com.sigsu.backend.auth.dtos;

public class LoginResponse {

    private final String token;
    private final String tipo;
    private final Long expiraEn;
    private final UsuarioResponse usuario;

    public LoginResponse(String token, String tipo, Long expiraEn, UsuarioResponse usuario) {
        this.token = token;
        this.tipo = tipo;
        this.expiraEn = expiraEn;
        this.usuario = usuario;
    }

    public String getToken() {
        return token;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getExpiraEn() {
        return expiraEn;
    }

    public UsuarioResponse getUsuario() {
        return usuario;
    }
}
