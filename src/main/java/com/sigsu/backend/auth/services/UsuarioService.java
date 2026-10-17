package com.sigsu.backend.auth.services;

import com.sigsu.backend.auth.dtos.CambiarRolRequest;
import com.sigsu.backend.auth.dtos.CrearUsuarioRequest;
import com.sigsu.backend.auth.dtos.UsuarioResponse;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.common.exceptions.ConflictException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private static final String DUPLICATE_EMAIL = "Ya existe un usuario registrado con ese correo";
    private static final String USER_NOT_FOUND = "Usuario no encontrado";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }

        UsuarioEntity user = new UsuarioEntity();
        user.setNombres(request.getNombres());
        user.setApellidos(request.getApellidos());
        user.setCorreo(request.getCorreo());
        user.setPasswordHash(passwordEncoder.encode(request.getContrasena()));
        user.setRol(request.getRol());
        user.setActivo(true);

        return UsuarioResponse.from(usuarioRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTecnicos(Integer areaId) {
        List<UsuarioEntity> tecnicos = areaId == null
                ? usuarioRepository.findByRolAndActivoTrueOrderByNombresAsc(RolEnum.TECNICO)
                : usuarioRepository.findByRolAndAreaIdAndActivoTrueOrderByNombresAsc(RolEnum.TECNICO, areaId);

        return tecnicos.stream()
                .map(UsuarioResponse::from)
                .toList();
    }

    @Transactional
    public UsuarioResponse cambiarRol(Integer userId, CambiarRolRequest request) {
        UsuarioEntity user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        user.setRol(request.getRol());

        return UsuarioResponse.from(user);
    }
}
