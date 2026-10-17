package com.sigsu.backend.catalogos.dtos;

import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;

public class CatalogoResponse {

    private final Integer id;
    private final String codigo;
    private final String nombre;
    private final Byte nivel;
    private final Boolean activo;

    public CatalogoResponse(Integer id, String codigo, String nombre, Byte nivel, Boolean activo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.nivel = nivel;
        this.activo = activo;
    }

    public static CatalogoResponse from(AreaEntity area) {
        return new CatalogoResponse(area.getId(), area.getCodigo(), area.getNombre(), null, area.getActivo());
    }

    public static CatalogoResponse from(CategoriaEntity categoria) {
        return new CatalogoResponse(categoria.getId(), categoria.getCodigo(), categoria.getNombre(), null,
                categoria.getActivo());
    }

    public static CatalogoResponse from(PrioridadEntity prioridad) {
        return new CatalogoResponse(prioridad.getId(), prioridad.getCodigo(), prioridad.getNombre(),
                prioridad.getNivel(), prioridad.getActivo());
    }

    public Integer getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public Byte getNivel() {
        return nivel;
    }

    public Boolean getActivo() {
        return activo;
    }
}
