package com.sigsu.backend.auth.services;

import com.sigsu.backend.auth.dtos.LoginRequest;
import com.sigsu.backend.auth.dtos.LoginResponse;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.common.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String CORREO = "ana@sigsu.local";
    private static final String CONTRASENA = "secreta123";
    private static final String HASH = "$2a$10$hash";
    private static final String TOKEN = "token-de-prueba";
    private static final long EXPIRATION_MS = 3_600_000L;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("UT-AUTH-01 rechaza un correo no registrado")
    void rejectsUnknownEmail() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(CORREO, CONTRASENA)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("UT-AUTH-02 rechaza una contraseña incorrecta")
    void rejectsWrongPassword() {
        UsuarioEntity user = user(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CONTRASENA, HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(CORREO, CONTRASENA)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciales incorrectas");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("UT-AUTH-03 rechaza un usuario desactivado")
    void rejectsInactiveUser() {
        UsuarioEntity user = user(false);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CONTRASENA, HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(CORREO, CONTRASENA)))
                .isInstanceOf(DisabledException.class)
                .hasMessage("El usuario está desactivado");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("UT-AUTH-04 devuelve token, tipo, expiración y usuario")
    void returnsLoginResponse() {
        UsuarioEntity user = user(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CONTRASENA, HASH)).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(TOKEN);
        when(jwtService.getExpirationMs()).thenReturn(EXPIRATION_MS);

        LoginResponse response = authService.login(new LoginRequest(CORREO, CONTRASENA));

        assertThat(response.getToken()).isEqualTo(TOKEN);
        assertThat(response.getTipo()).isEqualTo("Bearer");
        assertThat(response.getExpiraEn()).isEqualTo(EXPIRATION_MS);
        assertThat(response.getUsuario().getCorreo()).isEqualTo(CORREO);
        assertThat(response.getUsuario().getRol()).isEqualTo("SOLICITANTE");
    }

    @Test
    @DisplayName("UT-AUTH-05 un login fallido no emite token")
    void failedLoginDoesNotGenerateToken() {
        UsuarioEntity user = user(true);
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(CONTRASENA, HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(CORREO, CONTRASENA)))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    private UsuarioEntity user(boolean activo) {
        UsuarioEntity user = new UsuarioEntity();
        user.setNombres("Ana");
        user.setApellidos("Perez");
        user.setCorreo(CORREO);
        user.setPasswordHash(HASH);
        user.setRol(RolEnum.SOLICITANTE);
        user.setActivo(activo);
        return user;
    }
}
