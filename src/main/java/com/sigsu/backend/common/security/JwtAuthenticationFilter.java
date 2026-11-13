package com.sigsu.backend.common.security;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        authenticate(header.substring(BEARER_PREFIX.length()), request);
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (JwtException | IllegalArgumentException ex) {
            // Token inválido, vencido o vacío: rechazo habitual, sin datos del token ni stack trace.
            SecurityContextHolder.clearContext();
            return;
        } catch (RuntimeException ex) {
            technicalFailure("token_processing", ex);
            return;
        }

        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                return;
            }

            usuarioRepository.findByCorreo(email)
                    .filter(user -> Boolean.TRUE.equals(user.getActivo()))
                    .ifPresent(user -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, authorities(user));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        } catch (RuntimeException ex) {
            technicalFailure("user_lookup", ex);
        }
    }

    private void technicalFailure(String stage, RuntimeException ex) {
        SecurityContextHolder.clearContext();
        // Conservar los frames útiles, no mensajes/causas que puedan contener correo, SQL o JWT.
        RuntimeException safe = new RuntimeException("JWT technical failure: " + stage);
        safe.setStackTrace(ex.getStackTrace());
        LOG.atError().addKeyValue("event", "auth.jwt.technical_failure")
                .addKeyValue("stage", stage).addKeyValue("errorType", ex.getClass().getName())
                .setCause(safe).log("JWT authentication technical failure");
    }

    private List<SimpleGrantedAuthority> authorities(UsuarioEntity user) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRol().name()));
    }
}
