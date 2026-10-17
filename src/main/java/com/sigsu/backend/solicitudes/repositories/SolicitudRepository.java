package com.sigsu.backend.solicitudes.repositories;

import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<SolicitudEntity, Integer>, JpaSpecificationExecutor<SolicitudEntity> {

    Optional<SolicitudEntity> findByCodigo(String codigo);

    Optional<SolicitudEntity> findFirstByCodigoStartingWithOrderByCodigoDesc(String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select solicitud from SolicitudEntity solicitud where solicitud.id = :id")
    Optional<SolicitudEntity> findByIdForUpdate(@Param("id") Integer id);
}
