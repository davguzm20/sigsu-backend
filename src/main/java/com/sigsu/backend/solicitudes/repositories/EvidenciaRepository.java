package com.sigsu.backend.solicitudes.repositories;

import com.sigsu.backend.solicitudes.entities.EvidenciaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenciaRepository extends JpaRepository<EvidenciaEntity, Integer> {

    List<EvidenciaEntity> findBySolicitudIdOrderByCreadoEnAsc(Integer solicitudId);
}
