package com.sigsu.backend.catalogos.services;

import com.sigsu.backend.catalogos.dtos.CatalogoRequest;
import com.sigsu.backend.catalogos.dtos.CatalogoResponse;
import com.sigsu.backend.catalogos.dtos.PrioridadRequest;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.catalogos.repositories.CategoriaRepository;
import com.sigsu.backend.catalogos.repositories.PrioridadRepository;
import com.sigsu.backend.common.exceptions.ConflictException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoService {

    private static final String AREA_NOT_FOUND = "Área no encontrada";
    private static final String CATEGORIA_NOT_FOUND = "Categoría no encontrada";
    private static final String PRIORIDAD_NOT_FOUND = "Prioridad no encontrada";

    private final AreaRepository areaRepository;
    private final CategoriaRepository categoriaRepository;
    private final PrioridadRepository prioridadRepository;

    public CatalogoService(AreaRepository areaRepository, CategoriaRepository categoriaRepository,
                           PrioridadRepository prioridadRepository) {
        this.areaRepository = areaRepository;
        this.categoriaRepository = categoriaRepository;
        this.prioridadRepository = prioridadRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponse> listarAreas() {
        return areaRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(CatalogoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponse> listarCategorias() {
        return categoriaRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(CatalogoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogoResponse> listarPrioridades() {
        return prioridadRepository.findByActivoTrueOrderByNivelAsc().stream()
                .map(CatalogoResponse::from)
                .toList();
    }

    @Transactional
    public CatalogoResponse crearArea(CatalogoRequest request) {
        if (areaRepository.existsByCodigo(request.getCodigo())) {
            throw new ConflictException("Ya existe un área con ese código");
        }
        if (areaRepository.existsByNombre(request.getNombre())) {
            throw new ConflictException("Ya existe un área con ese nombre");
        }

        AreaEntity area = new AreaEntity();
        area.setCodigo(request.getCodigo());
        area.setNombre(request.getNombre());
        area.setActivo(request.getActivo() == null || request.getActivo());

        return CatalogoResponse.from(areaRepository.save(area));
    }

    @Transactional
    public CatalogoResponse crearCategoria(CatalogoRequest request) {
        if (categoriaRepository.existsByCodigo(request.getCodigo())) {
            throw new ConflictException("Ya existe una categoría con ese código");
        }
        if (categoriaRepository.existsByNombre(request.getNombre())) {
            throw new ConflictException("Ya existe una categoría con ese nombre");
        }

        CategoriaEntity categoria = new CategoriaEntity();
        categoria.setCodigo(request.getCodigo());
        categoria.setNombre(request.getNombre());
        categoria.setActivo(request.getActivo() == null || request.getActivo());

        return CatalogoResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CatalogoResponse crearPrioridad(PrioridadRequest request) {
        if (prioridadRepository.existsByCodigo(request.getCodigo())) {
            throw new ConflictException("Ya existe una prioridad con ese código");
        }
        if (prioridadRepository.existsByNombre(request.getNombre())) {
            throw new ConflictException("Ya existe una prioridad con ese nombre");
        }
        if (prioridadRepository.existsByNivel(request.getNivel().byteValue())) {
            throw new ConflictException("Ya existe una prioridad con ese nivel");
        }

        PrioridadEntity prioridad = new PrioridadEntity();
        prioridad.setCodigo(request.getCodigo());
        prioridad.setNombre(request.getNombre());
        prioridad.setNivel(request.getNivel().byteValue());
        prioridad.setActivo(request.getActivo() == null || request.getActivo());

        return CatalogoResponse.from(prioridadRepository.save(prioridad));
    }

    @Transactional
    public CatalogoResponse actualizarArea(Integer id, CatalogoRequest request) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));

        area.setCodigo(request.getCodigo());
        area.setNombre(request.getNombre());
        if (request.getActivo() != null) {
            area.setActivo(request.getActivo());
        }

        return CatalogoResponse.from(area);
    }

    @Transactional
    public CatalogoResponse actualizarCategoria(Integer id, CatalogoRequest request) {
        CategoriaEntity categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORIA_NOT_FOUND));

        categoria.setCodigo(request.getCodigo());
        categoria.setNombre(request.getNombre());
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }

        return CatalogoResponse.from(categoria);
    }

    @Transactional
    public CatalogoResponse actualizarPrioridad(Integer id, PrioridadRequest request) {
        PrioridadEntity prioridad = prioridadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRIORIDAD_NOT_FOUND));

        prioridad.setCodigo(request.getCodigo());
        prioridad.setNombre(request.getNombre());
        prioridad.setNivel(request.getNivel().byteValue());
        if (request.getActivo() != null) {
            prioridad.setActivo(request.getActivo());
        }

        return CatalogoResponse.from(prioridad);
    }

    @Transactional
    public void eliminarArea(Integer id) {
        AreaEntity area = areaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));

        areaRepository.delete(area);
        areaRepository.flush();
    }

    @Transactional
    public void eliminarCategoria(Integer id) {
        CategoriaEntity categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(CATEGORIA_NOT_FOUND));

        categoriaRepository.delete(categoria);
        categoriaRepository.flush();
    }

    @Transactional
    public void eliminarPrioridad(Integer id) {
        PrioridadEntity prioridad = prioridadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRIORIDAD_NOT_FOUND));

        prioridadRepository.delete(prioridad);
        prioridadRepository.flush();
    }
}
