package com.sigsu.backend.catalogos.repositories;

import com.sigsu.backend.catalogos.entities.AreaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<AreaEntity, Integer> {

    List<AreaEntity> findByActivoTrueOrderByNombreAsc();

    Optional<AreaEntity> findByIdAndActivoTrue(Integer id);
}
