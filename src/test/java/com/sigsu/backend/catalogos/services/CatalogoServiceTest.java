package com.sigsu.backend.catalogos.services;

import com.sigsu.backend.catalogos.dtos.CatalogoRequest;
import com.sigsu.backend.catalogos.dtos.PrioridadRequest;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.catalogos.repositories.CategoriaRepository;
import com.sigsu.backend.catalogos.repositories.PrioridadRepository;
import com.sigsu.backend.common.exceptions.ConflictException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private PrioridadRepository prioridadRepository;

    @InjectMocks
    private CatalogoService catalogoService;

    @Test
    @DisplayName("UT-CAT-01 rechaza un área con código duplicado")
    void rejectsDuplicateAreaCode() {
        when(areaRepository.existsByCodigo("SISTEMAS")).thenReturn(true);

        assertThatThrownBy(() -> catalogoService.crearArea(request("SISTEMAS", "Sistemas")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un área con ese código");

        verify(areaRepository).existsByCodigo("SISTEMAS");
    }

    @Test
    @DisplayName("UT-CAT-02 rechaza un área con nombre duplicado")
    void rejectsDuplicateAreaName() {
        when(areaRepository.existsByCodigo("SISTEMAS")).thenReturn(false);
        when(areaRepository.existsByNombre("Sistemas")).thenReturn(true);

        assertThatThrownBy(() -> catalogoService.crearArea(request("SISTEMAS", "Sistemas")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un área con ese nombre");

        verify(areaRepository).existsByNombre("Sistemas");
    }

    @Test
    @DisplayName("UT-CAT-03 rechaza una prioridad con nivel duplicado")
    void rejectsDuplicatePriorityLevel() {
        PrioridadRequest request = new PrioridadRequest();
        request.setCodigo("ALTA");
        request.setNombre("Alta");
        request.setNivel(3);
        when(prioridadRepository.existsByCodigo("ALTA")).thenReturn(false);
        when(prioridadRepository.existsByNombre("Alta")).thenReturn(false);
        when(prioridadRepository.existsByNivel((byte) 3)).thenReturn(true);

        assertThatThrownBy(() -> catalogoService.crearPrioridad(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe una prioridad con ese nivel");

        verify(prioridadRepository).existsByNivel((byte) 3);
    }

    @Test
    @DisplayName("UT-CAT-04 rechaza actualizar un catálogo inexistente")
    void rejectsUpdateOfUnknownCatalog() {
        when(areaRepository.findById(9999)).thenReturn(Optional.empty());
        when(categoriaRepository.findById(9999)).thenReturn(Optional.empty());
        when(prioridadRepository.findById(9999)).thenReturn(Optional.empty());

        CatalogoRequest areaRequest = request("SISTEMAS", "Sistemas");
        PrioridadRequest prioridadRequest = new PrioridadRequest();
        prioridadRequest.setCodigo("ALTA");
        prioridadRequest.setNombre("Alta");
        prioridadRequest.setNivel(3);

        assertThatThrownBy(() -> catalogoService.actualizarArea(9999, areaRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Área no encontrada");
        assertThatThrownBy(() -> catalogoService.actualizarCategoria(9999, areaRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoría no encontrada");
        assertThatThrownBy(() -> catalogoService.actualizarPrioridad(9999, prioridadRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Prioridad no encontrada");
    }

    private CatalogoRequest request(String codigo, String nombre) {
        CatalogoRequest request = new CatalogoRequest();
        request.setCodigo(codigo);
        request.setNombre(nombre);
        return request;
    }
}
