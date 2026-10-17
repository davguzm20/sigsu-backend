package com.sigsu.backend.dashboard.dtos;

public class IndicadorResponse {

    private final Integer id;
    private final String nombre;
    private final Long total;

    public IndicadorResponse(Integer id, String nombre, Long total) {
        this.id = id;
        this.nombre = nombre;
        this.total = total;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Long getTotal() {
        return total;
    }
}
