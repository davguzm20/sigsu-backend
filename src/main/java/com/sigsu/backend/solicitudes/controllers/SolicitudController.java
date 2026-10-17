package com.sigsu.backend.solicitudes.controllers;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.solicitudes.dtos.CrearSeguimientoRequest;
import com.sigsu.backend.solicitudes.dtos.CrearSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.SeguimientoResponse;
import com.sigsu.backend.solicitudes.dtos.SolicitudResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.services.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;

    public SolicitudController(SolicitudService solicitudService) {
        this.solicitudService = solicitudService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SOLICITANTE')")
    public ResponseEntity<SolicitudResponse> register(@Valid @RequestBody CrearSolicitudRequest request,
                                                      @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.register(request, user));
    }

    @GetMapping
    public ResponseEntity<List<SolicitudResponse>> search(
            @AuthenticationPrincipal UsuarioEntity user,
            @RequestParam(required = false) EstadoSolicitudEnum estado,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(solicitudService.search(user, estado, texto, desde, hasta));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> findDetail(@PathVariable Integer id,
                                                        @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findDetail(id, user));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<SolicitudResponse> findDetailByCodigo(@PathVariable String codigo,
                                                                @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findDetailByCodigo(codigo, user));
    }

    @PostMapping("/{id}/seguimientos")
    public ResponseEntity<SeguimientoResponse> addSeguimiento(@PathVariable Integer id,
                                                              @Valid @RequestBody CrearSeguimientoRequest request,
                                                              @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.addSeguimiento(id, request, user));
    }
}
