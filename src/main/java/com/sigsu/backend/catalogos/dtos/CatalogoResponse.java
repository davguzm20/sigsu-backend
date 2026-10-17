package com.sigsu.backend.catalogos.dtos;

import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;

public class CatalogoResponse {

    private final Integer id;
    private final String codigo;
    private final String nombre;

    public CatalogoResponse(Integer id, String codigo, String nombre) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public static CatalogoResponse from(AreaEntity area) {
        return new CatalogoResponse(area.getId(), area.getCodigo(), area.getNombre());
    }

    public static CatalogoResponse from(CategoriaEntity categoria) {
        return new CatalogoResponse(categoria.getId(), categoria.getCodigo(), categoria.getNombre());
    }

    public static CatalogoResponse from(PrioridadEntity prioridad) {
        return new CatalogoResponse(prioridad.getId(), prioridad.getCodigo(), prioridad.getNombre());
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
}
