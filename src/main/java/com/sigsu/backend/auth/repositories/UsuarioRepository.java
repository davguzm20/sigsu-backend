package com.sigsu.backend.auth.repositories;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {

    Optional<UsuarioEntity> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Integer id);

    List<UsuarioEntity> findByRolAndActivoTrueOrderByNombresAsc(RolEnum rol);

    List<UsuarioEntity> findByRolAndAreaIdAndActivoTrueOrderByNombresAsc(RolEnum rol, Integer areaId);
}
