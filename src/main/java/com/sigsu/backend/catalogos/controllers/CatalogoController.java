package com.sigsu.backend.catalogos.controllers;

import com.sigsu.backend.catalogos.dtos.CatalogoRequest;
import com.sigsu.backend.catalogos.dtos.CatalogoResponse;
import com.sigsu.backend.catalogos.dtos.PrioridadRequest;
import com.sigsu.backend.catalogos.services.CatalogoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping("/api/categorias")
    public ResponseEntity<List<CatalogoResponse>> categorias() {
        return ResponseEntity.ok(catalogoService.listarCategorias());
    }

    @GetMapping("/api/prioridades")
    public ResponseEntity<List<CatalogoResponse>> prioridades() {
        return ResponseEntity.ok(catalogoService.listarPrioridades());
    }

    @GetMapping("/api/areas")
    public ResponseEntity<List<CatalogoResponse>> areas() {
        return ResponseEntity.ok(catalogoService.listarAreas());
    }

    @PostMapping("/api/categorias")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CatalogoResponse> crearCategoria(@Valid @RequestBody CatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearCategoria(request));
    }

    @PutMapping("/api/categorias/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CatalogoResponse> actualizarCategoria(@PathVariable Integer id,
                                                                @Valid @RequestBody CatalogoRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarCategoria(id, request));
    }

    @DeleteMapping("/api/categorias/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarCategoria(@PathVariable Integer id) {
        catalogoService.eliminarCategoria(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/prioridades")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CatalogoResponse> crearPrioridad(@Valid @RequestBody PrioridadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearPrioridad(request));
    }

    @PutMapping("/api/prioridades/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CatalogoResponse> actualizarPrioridad(@PathVariable Integer id,
                                                                @Valid @RequestBody PrioridadRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarPrioridad(id, request));
    }

    @DeleteMapping("/api/prioridades/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarPrioridad(@PathVariable Integer id) {
        catalogoService.eliminarPrioridad(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/areas")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CatalogoResponse> crearArea(@Valid @RequestBody CatalogoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoService.crearArea(request));
    }

    @PutMapping("/api/areas/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CatalogoResponse> actualizarArea(@PathVariable Integer id,
                                                           @Valid @RequestBody CatalogoRequest request) {
        return ResponseEntity.ok(catalogoService.actualizarArea(id, request));
    }

    @DeleteMapping("/api/areas/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarArea(@PathVariable Integer id) {
        catalogoService.eliminarArea(id);
        return ResponseEntity.noContent().build();
    }
}
