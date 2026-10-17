package com.sigsu.backend.auth.controllers;

import com.sigsu.backend.auth.dtos.ActualizarUsuarioRequest;
import com.sigsu.backend.auth.dtos.CambiarRolRequest;
import com.sigsu.backend.auth.dtos.CrearUsuarioRequest;
import com.sigsu.backend.auth.dtos.UsuarioResponse;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Registra un usuario nuevo")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.registrar(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Lista todos los usuarios")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/tecnicos")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMINISTRADOR')")
    @Operation(summary = "Lista los técnicos activos, con filtro opcional por área")
    public ResponseEntity<List<UsuarioResponse>> listarTecnicos(@RequestParam(required = false) Integer areaId) {
        return ResponseEntity.ok(usuarioService.listarTecnicos(areaId));
    }

    @GetMapping("/roles")
    @Operation(summary = "Lista los roles disponibles")
    public ResponseEntity<List<String>> roles() {
        return ResponseEntity.ok(Arrays.stream(RolEnum.values()).map(Enum::name).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Actualiza los datos de un usuario")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody ActualizarUsuarioRequest request,
                                                      @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Elimina un usuario que no tenga actividad")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id,
                                         @AuthenticationPrincipal UsuarioEntity user) {
        usuarioService.eliminar(id, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cambia el rol de un usuario")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Integer id,
                                                      @Valid @RequestBody CambiarRolRequest request,
                                                      @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(usuarioService.cambiarRol(id, request, user));
    }
}
