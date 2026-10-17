package com.sigsu.backend.solicitudes.controllers;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.solicitudes.dtos.CambiarEstadoRequest;
import com.sigsu.backend.solicitudes.dtos.ClasificarSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.CrearSeguimientoRequest;
import com.sigsu.backend.solicitudes.dtos.CrearSolicitudRequest;
import com.sigsu.backend.solicitudes.dtos.EvidenciaResponse;
import com.sigsu.backend.solicitudes.dtos.HistorialEstadoResponse;
import com.sigsu.backend.solicitudes.dtos.SeguimientoResponse;
import com.sigsu.backend.solicitudes.dtos.SolicitudResponse;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.services.EvidenciaService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final EvidenciaService evidenciaService;

    public SolicitudController(SolicitudService solicitudService, EvidenciaService evidenciaService) {
        this.solicitudService = solicitudService;
        this.evidenciaService = evidenciaService;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer prioridadId,
            @RequestParam(required = false) Integer responsableId,
            @RequestParam(required = false) Boolean sinAsignar) {
        return ResponseEntity.ok(solicitudService.search(user, estado, texto, desde, hasta, categoriaId, prioridadId,
                responsableId, sinAsignar));
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

    @PutMapping("/{id}/clasificacion")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<SolicitudResponse> clasificar(@PathVariable Integer id,
                                                        @Valid @RequestBody ClasificarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.clasificar(id, request));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'TECNICO')")
    public ResponseEntity<SolicitudResponse> cambiarEstado(@PathVariable Integer id,
                                                           @Valid @RequestBody CambiarEstadoRequest request,
                                                           @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.cambiarEstado(id, request, user));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoResponse>> findHistorial(@PathVariable Integer id,
                                                                       @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findHistorial(id, user));
    }

    @GetMapping("/estados")
    public ResponseEntity<List<String>> estados() {
        return ResponseEntity.ok(Arrays.stream(EstadoSolicitudEnum.values()).map(Enum::name).toList());
    }

    @PostMapping("/{id}/seguimientos")
    public ResponseEntity<SeguimientoResponse> addSeguimiento(@PathVariable Integer id,
                                                              @Valid @RequestBody CrearSeguimientoRequest request,
                                                              @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.addSeguimiento(id, request, user));
    }

    @PostMapping("/{id}/evidencias")
    public ResponseEntity<EvidenciaResponse> agregarEvidencia(@PathVariable Integer id,
                                                              @RequestParam("archivo") MultipartFile archivo,
                                                              @RequestParam(required = false) Integer seguimientoId,
                                                              @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evidenciaService.agregar(id, archivo, seguimientoId, user));
    }

    @GetMapping("/{id}/evidencias")
    public ResponseEntity<List<EvidenciaResponse>> findEvidencias(@PathVariable Integer id,
                                                                  @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(evidenciaService.listar(id, user));
    }
}
