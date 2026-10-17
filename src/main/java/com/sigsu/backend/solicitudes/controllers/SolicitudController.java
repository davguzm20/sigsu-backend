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
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Registra una solicitud")
    public ResponseEntity<SolicitudResponse> register(@Valid @RequestBody CrearSolicitudRequest request,
                                                      @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.register(request, user));
    }

    @GetMapping
    @Operation(summary = "Lista las solicitudes según el rol y los filtros")
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
    @Operation(summary = "Devuelve el detalle de una solicitud")
    public ResponseEntity<SolicitudResponse> findDetail(@PathVariable Integer id,
                                                        @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findDetail(id, user));
    }

    @GetMapping("/codigo/{codigo}")
    @Operation(summary = "Devuelve el detalle por el código de la solicitud")
    public ResponseEntity<SolicitudResponse> findDetailByCodigo(@PathVariable String codigo,
                                                                @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findDetailByCodigo(codigo, user));
    }

    @PutMapping("/{id}/clasificacion")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Clasifica la solicitud con categoría, prioridad, área y plazo")
    public ResponseEntity<SolicitudResponse> clasificar(@PathVariable Integer id,
                                                        @Valid @RequestBody ClasificarSolicitudRequest request) {
        return ResponseEntity.ok(solicitudService.clasificar(id, request));
    }

    @PutMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'TECNICO')")
    @Operation(summary = "Cambia el estado según la transición válida para el rol")
    public ResponseEntity<SolicitudResponse> cambiarEstado(@PathVariable Integer id,
                                                           @Valid @RequestBody CambiarEstadoRequest request,
                                                           @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.cambiarEstado(id, request, user));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Devuelve el historial de estados de la solicitud")
    public ResponseEntity<List<HistorialEstadoResponse>> findHistorial(@PathVariable Integer id,
                                                                       @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findHistorial(id, user));
    }

    @GetMapping("/estados")
    @Operation(summary = "Lista los estados del ciclo de vida")
    public ResponseEntity<List<String>> estados() {
        return ResponseEntity.ok(Arrays.stream(EstadoSolicitudEnum.values()).map(Enum::name).toList());
    }

    @PostMapping("/{id}/seguimientos")
    @Operation(summary = "Agrega un comentario a la solicitud")
    public ResponseEntity<SeguimientoResponse> addSeguimiento(@PathVariable Integer id,
                                                              @Valid @RequestBody CrearSeguimientoRequest request,
                                                              @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(solicitudService.addSeguimiento(id, request, user));
    }

    @GetMapping("/{id}/seguimientos")
    @Operation(summary = "Lista los comentarios de la solicitud")
    public ResponseEntity<List<SeguimientoResponse>> findSeguimientos(@PathVariable Integer id,
                                                                      @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(solicitudService.findSeguimientos(id, user));
    }

    @PostMapping("/{id}/evidencias")
    @Operation(summary = "Adjunta una evidencia a la solicitud")
    public ResponseEntity<EvidenciaResponse> agregarEvidencia(@PathVariable Integer id,
                                                              @RequestParam("archivo") MultipartFile archivo,
                                                              @RequestParam(required = false) Integer seguimientoId,
                                                              @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(evidenciaService.agregar(id, archivo, seguimientoId, user));
    }

    @GetMapping("/{id}/evidencias")
    @Operation(summary = "Lista las evidencias de la solicitud")
    public ResponseEntity<List<EvidenciaResponse>> findEvidencias(@PathVariable Integer id,
                                                                  @AuthenticationPrincipal UsuarioEntity user) {
        return ResponseEntity.ok(evidenciaService.listar(id, user));
    }
}
