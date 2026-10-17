package com.sigsu.backend.catalogos.repositories;

import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PrioridadRepository extends JpaRepository<PrioridadEntity, Integer> {

    List<PrioridadEntity> findByActivoTrueOrderByIdAsc();

    Optional<PrioridadEntity> findByIdAndActivoTrue(Integer id);
}
