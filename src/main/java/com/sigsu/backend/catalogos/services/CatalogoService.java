package com.sigsu.backend.catalogos.services;

import com.sigsu.backend.catalogos.dtos.CatalogoResponse;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.catalogos.repositories.CategoriaRepository;
import com.sigsu.backend.catalogos.repositories.PrioridadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogoService {

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
        return prioridadRepository.findByActivoTrueOrderByIdAsc().stream()
                .map(CatalogoResponse::from)
                .toList();
    }
}
