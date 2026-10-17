package com.sigsu.backend.catalogos.controllers;

import com.sigsu.backend.catalogos.dtos.CatalogoResponse;
import com.sigsu.backend.catalogos.services.CatalogoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
