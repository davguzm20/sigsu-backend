package com.sigsu.backend.dashboard.dtos;

import java.util.List;

public class DashboardResponse {

    private final Long total;
    private final Long pendientes;
    private final Long atendidas;
    private final Long vencidas;
    private final Double porcentajeResuelto;
    private final Double tiempoPromedioAtencionHoras;
    private final List<IndicadorResponse> porCategoria;
    private final List<IndicadorResponse> porPrioridad;
    private final List<IndicadorResponse> porResponsable;

    public DashboardResponse(Long total, Long pendientes, Long atendidas, Long vencidas, Double porcentajeResuelto,
                             Double tiempoPromedioAtencionHoras, List<IndicadorResponse> porCategoria,
                             List<IndicadorResponse> porPrioridad, List<IndicadorResponse> porResponsable) {
        this.total = total;
        this.pendientes = pendientes;
        this.atendidas = atendidas;
        this.vencidas = vencidas;
        this.porcentajeResuelto = porcentajeResuelto;
        this.tiempoPromedioAtencionHoras = tiempoPromedioAtencionHoras;
        this.porCategoria = porCategoria;
        this.porPrioridad = porPrioridad;
        this.porResponsable = porResponsable;
    }

    public Long getTotal() {
        return total;
    }

    public Long getPendientes() {
        return pendientes;
    }

    public Long getAtendidas() {
        return atendidas;
    }

    public Long getVencidas() {
        return vencidas;
    }

    public Double getPorcentajeResuelto() {
        return porcentajeResuelto;
    }

    public Double getTiempoPromedioAtencionHoras() {
        return tiempoPromedioAtencionHoras;
    }

    public List<IndicadorResponse> getPorCategoria() {
        return porCategoria;
    }

    public List<IndicadorResponse> getPorPrioridad() {
        return porPrioridad;
    }

    public List<IndicadorResponse> getPorResponsable() {
        return porResponsable;
    }
}
