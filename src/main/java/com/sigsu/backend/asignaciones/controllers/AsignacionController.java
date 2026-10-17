package com.sigsu.backend.asignaciones.controllers;

import com.sigsu.backend.asignaciones.dtos.AsignacionResponse;
import com.sigsu.backend.asignaciones.dtos.AsignarTecnicoRequest;
import com.sigsu.backend.asignaciones.services.AsignacionService;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes/{solicitudId}/asignaciones")
public class AsignacionController {

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<AsignacionResponse> asignar(@PathVariable Integer solicitudId,
                                                      @Valid @RequestBody AsignarTecnicoRequest request,
                                                      @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(asignacionService.asignar(solicitudId, request, user));
    }
}
