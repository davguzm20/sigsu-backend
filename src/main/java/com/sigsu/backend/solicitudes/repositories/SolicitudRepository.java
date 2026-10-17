package com.sigsu.backend.solicitudes.repositories;

import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<SolicitudEntity, Integer>, JpaSpecificationExecutor<SolicitudEntity> {

    Optional<SolicitudEntity> findByCodigo(String codigo);

    Optional<SolicitudEntity> findFirstByCodigoStartingWithOrderByCodigoDesc(String prefix);
}
