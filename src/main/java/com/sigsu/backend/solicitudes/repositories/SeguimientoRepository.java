package com.sigsu.backend.solicitudes.repositories;

import com.sigsu.backend.solicitudes.entities.SeguimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeguimientoRepository extends JpaRepository<SeguimientoEntity, Integer> {

    List<SeguimientoEntity> findBySolicitudIdOrderByCreadoEnAsc(Integer solicitudId);
}
