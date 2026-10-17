package com.sigsu.backend.auth.controllers;

import com.sigsu.backend.auth.dtos.LoginRequest;
import com.sigsu.backend.auth.dtos.LoginResponse;
import com.sigsu.backend.auth.dtos.UsuarioResponse;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesión y devuelve el token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/perfil")
    @Operation(summary = "Devuelve el usuario autenticado")
    public ResponseEntity<UsuarioResponse> getProfile(@AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(authService.getAuthenticatedUser(user));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cierra sesión y responde 204 sin tocar el servidor")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
