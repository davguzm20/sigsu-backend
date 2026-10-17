package com.sigsu.backend.solicitudes.repositories;

import com.sigsu.backend.solicitudes.entities.HistorialEstadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstadoEntity, Integer> {

    List<HistorialEstadoEntity> findBySolicitudIdOrderByCambiadoEnAsc(Integer solicitudId);
}
