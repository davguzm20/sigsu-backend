package com.sigsu.backend.common.security;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("UT-FIL-01 token válido autentica con el rol")
    void authenticatesWithValidToken() throws ServletException, IOException {
        UsuarioEntity user = user(RolEnum.TECNICO, true, "tecnico@sigsu.local");
        when(jwtService.extractEmail("token")).thenReturn("tecnico@sigsu.local");
        when(usuarioRepository.findByCorreo("tecnico@sigsu.local")).thenReturn(Optional.of(user));

        MockHttpServletRequest request = request("Bearer token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TECNICO");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("UT-FIL-02 token inválido deja el contexto vacío")
    void ignoresInvalidToken() throws ServletException, IOException {
        when(jwtService.extractEmail("roto")).thenThrow(new MalformedJwtException("roto"));

        MockHttpServletRequest request = request("Bearer roto");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verify(usuarioRepository, never()).findByCorreo(anyString());
    }

    @Test
    @DisplayName("UT-FIL-03 usuario inactivo no autentica")
    void ignoresInactiveUser() throws ServletException, IOException {
        when(jwtService.extractEmail("token")).thenReturn("inactivo@sigsu.local");
        when(usuarioRepository.findByCorreo("inactivo@sigsu.local"))
                .thenReturn(Optional.of(user(RolEnum.SOLICITANTE, false, "inactivo@sigsu.local")));

        MockHttpServletRequest request = request("Bearer token");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        return request;
    }

    private UsuarioEntity user(RolEnum rol, boolean activo, String correo) {
        UsuarioEntity user = new UsuarioEntity();
        user.setNombres("Ana");
        user.setApellidos("Perez");
        user.setCorreo(correo);
        user.setRol(rol);
        user.setActivo(activo);
        return user;
    }
}
