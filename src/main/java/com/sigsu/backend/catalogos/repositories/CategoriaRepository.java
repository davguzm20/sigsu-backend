package com.sigsu.backend.catalogos.repositories;

import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<CategoriaEntity, Integer> {

    List<CategoriaEntity> findByActivoTrueOrderByNombreAsc();

    Optional<CategoriaEntity> findByIdAndActivoTrue(Integer id);
}
