package com.sigsu.backend.auth.services;

import com.sigsu.backend.auth.dtos.ActualizarUsuarioRequest;
import com.sigsu.backend.auth.dtos.CambiarRolRequest;
import com.sigsu.backend.auth.dtos.CrearUsuarioRequest;
import com.sigsu.backend.auth.dtos.UsuarioResponse;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.common.exceptions.BusinessRuleException;
import com.sigsu.backend.common.exceptions.ConflictException;
import com.sigsu.backend.common.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("UT-USR-01 rechaza un correo duplicado al registrar")
    void rejectsDuplicateEmailOnRegistration() {
        CrearUsuarioRequest request = new CrearUsuarioRequest("Ana", "Perez", "ana@sigsu.local", "secreta123",
                RolEnum.SOLICITANTE);
        when(usuarioRepository.existsByCorreo("ana@sigsu.local")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.registrar(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un usuario registrado con ese correo");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("UT-USR-02 rechaza desactivar la cuenta propia")
    void rejectsSelfDeactivation() {
        UsuarioEntity user = user(1, RolEnum.ADMINISTRADOR);
        UsuarioEntity currentUser = user(1, RolEnum.ADMINISTRADOR);
        ActualizarUsuarioRequest request = updateRequest("ana@sigsu.local", false);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));
        when(usuarioRepository.existsByCorreoAndIdNot("ana@sigsu.local", 1)).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.actualizar(1, request, currentUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No puede desactivar su propia cuenta");
    }

    @Test
    @DisplayName("UT-USR-03 rechaza eliminar la cuenta propia")
    void rejectsSelfDeletion() {
        UsuarioEntity user = user(1, RolEnum.ADMINISTRADOR);
        UsuarioEntity currentUser = user(1, RolEnum.ADMINISTRADOR);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> usuarioService.eliminar(1, currentUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No puede eliminar su propia cuenta");

        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("UT-USR-04 rechaza quitarse el rol de administrador")
    void rejectsRemovingOwnAdminRole() {
        UsuarioEntity user = user(1, RolEnum.ADMINISTRADOR);
        UsuarioEntity currentUser = user(1, RolEnum.ADMINISTRADOR);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> usuarioService.cambiarRol(1, new CambiarRolRequest(RolEnum.TECNICO), currentUser))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("No puede quitarse a sí mismo el rol de administrador");

        assertThat(user.getRol()).isEqualTo(RolEnum.ADMINISTRADOR);
    }

    @Test
    @DisplayName("UT-USR-05 cambia el rol de otro usuario")
    void changesAnotherUserRole() {
        UsuarioEntity user = user(2, RolEnum.TECNICO);
        UsuarioEntity currentUser = user(1, RolEnum.ADMINISTRADOR);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(user));

        UsuarioResponse response = usuarioService.cambiarRol(2, new CambiarRolRequest(RolEnum.SUPERVISOR), currentUser);

        assertThat(response.getRol()).isEqualTo("SUPERVISOR");
        assertThat(user.getRol()).isEqualTo(RolEnum.SUPERVISOR);
    }

    @Test
    @DisplayName("UT-USR-06 rechaza actualizar un usuario inexistente")
    void rejectsUpdateOfUnknownUser() {
        when(usuarioRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.actualizar(9999, updateRequest("ana@sigsu.local", true),
                user(1, RolEnum.ADMINISTRADOR)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario no encontrado");
    }

    @Test
    @DisplayName("UT-USR-07 rechaza actualizar con un correo duplicado")
    void rejectsUpdateWithDuplicateEmail() {
        UsuarioEntity user = user(2, RolEnum.TECNICO);
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(user));
        when(usuarioRepository.existsByCorreoAndIdNot("otro@sigsu.local", 2)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.actualizar(2, updateRequest("otro@sigsu.local", true),
                user(1, RolEnum.ADMINISTRADOR)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe un usuario registrado con ese correo");
    }

    @Test
    @DisplayName("UT-USR-08 lista solo los técnicos activos del área")
    void listsActiveTechniciansByArea() {
        UsuarioEntity ana = user(2, RolEnum.TECNICO);
        UsuarioEntity luis = user(3, RolEnum.TECNICO);
        when(usuarioRepository.findByRolAndAreaIdAndActivoTrueOrderByNombresAsc(RolEnum.TECNICO, 5))
                .thenReturn(List.of(ana, luis));

        List<UsuarioResponse> response = usuarioService.listarTecnicos(5);

        assertThat(response).hasSize(2);
        assertThat(response).extracting(UsuarioResponse::getRol).containsOnly("TECNICO");
        verify(usuarioRepository).findByRolAndAreaIdAndActivoTrueOrderByNombresAsc(RolEnum.TECNICO, 5);
    }

    private UsuarioEntity user(Integer id, RolEnum rol) {
        UsuarioEntity user = new UsuarioEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setNombres("Ana");
        user.setApellidos("Perez");
        user.setCorreo("ana@sigsu.local");
        user.setPasswordHash("$2a$10$hash");
        user.setRol(rol);
        user.setActivo(true);
        return user;
    }

    private ActualizarUsuarioRequest updateRequest(String correo, boolean activo) {
        ActualizarUsuarioRequest request = new ActualizarUsuarioRequest();
        request.setNombres("Ana");
        request.setApellidos("Perez");
        request.setCorreo(correo);
        request.setActivo(activo);
        return request;
    }
}
