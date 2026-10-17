package com.sigsu.backend.dashboard.controllers;

import com.sigsu.backend.dashboard.dtos.DashboardResponse;
import com.sigsu.backend.dashboard.services.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMINISTRADOR')")
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer prioridadId) {
        return ResponseEntity.ok(dashboardService.build(desde, hasta, categoriaId, prioridadId));
    }
}
