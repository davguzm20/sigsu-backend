package com.sigsu.backend.asignaciones.repositories;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionRepository extends JpaRepository<AsignacionEntity, Integer> {

    boolean existsBySolicitudIdAndFinalizadaEnIsNull(Integer solicitudId);

    boolean existsBySolicitudIdAndTecnicoIdAndFinalizadaEnIsNull(Integer solicitudId, Integer tecnicoId);

    Optional<AsignacionEntity> findFirstBySolicitudIdAndFinalizadaEnIsNull(Integer solicitudId);

    List<AsignacionEntity> findBySolicitudIdInAndFinalizadaEnIsNull(List<Integer> solicitudIds);
}
