package com.sigsu.backend.auth.services;

import com.sigsu.backend.auth.dtos.ActualizarUsuarioRequest;
import com.sigsu.backend.auth.dtos.CambiarRolRequest;
import com.sigsu.backend.auth.dtos.CrearUsuarioRequest;
import com.sigsu.backend.auth.dtos.UsuarioResponse;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
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
    private static final String AREA_NOT_FOUND = "Área no encontrada";
    private static final String CANNOT_DEACTIVATE_SELF = "No puede desactivar su propia cuenta";
    private static final String CANNOT_DELETE_SELF = "No puede eliminar su propia cuenta";
    private static final String CANNOT_REMOVE_OWN_ADMIN = "No puede quitarse a sí mismo el rol de administrador";

    private final UsuarioRepository usuarioRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, AreaRepository areaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.areaRepository = areaRepository;
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
    public UsuarioResponse actualizar(Integer userId, ActualizarUsuarioRequest request, UsuarioEntity currentUser) {
        UsuarioEntity user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (usuarioRepository.existsByCorreoAndIdNot(request.getCorreo(), userId)) {
            throw new ConflictException(DUPLICATE_EMAIL);
        }
        if (request.getActivo() != null && !request.getActivo() && user.getId().equals(currentUser.getId())) {
            throw new BusinessRuleException(CANNOT_DEACTIVATE_SELF);
        }

        user.setNombres(request.getNombres());
        user.setApellidos(request.getApellidos());
        user.setCorreo(request.getCorreo());

        if (request.getAreaId() != null) {
            AreaEntity area = areaRepository.findById(request.getAreaId())
                    .orElseThrow(() -> new ResourceNotFoundException(AREA_NOT_FOUND));
            user.setArea(area);
        }
        if (request.getActivo() != null) {
            user.setActivo(request.getActivo());
        }

        return UsuarioResponse.from(user);
    }

    @Transactional
    public void eliminar(Integer userId, UsuarioEntity currentUser) {
        UsuarioEntity user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (user.getId().equals(currentUser.getId())) {
            throw new BusinessRuleException(CANNOT_DELETE_SELF);
        }

        usuarioRepository.delete(user);
        usuarioRepository.flush();
    }

    @Transactional
    public UsuarioResponse cambiarRol(Integer userId, CambiarRolRequest request, UsuarioEntity currentUser) {
        UsuarioEntity user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND));

        if (user.getId().equals(currentUser.getId()) && request.getRol() != RolEnum.ADMINISTRADOR) {
            throw new BusinessRuleException(CANNOT_REMOVE_OWN_ADMIN);
        }

        user.setRol(request.getRol());

        return UsuarioResponse.from(user);
    }
}
