package com.sigsu.backend.common.security;

import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String OTHER_SECRET = "YWJjZGVmMDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODk=";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, EXPIRATION_MS));
    }

    @Test
    @DisplayName("UT-JWT-01 genera un token con sub, id, iat y exp")
    void generatesTokenWithClaims() {
        String token = jwtService.generateToken(user());
        Claims claims = parse(token);

        assertThat(claims.getSubject()).isEqualTo("ana@sigsu.local");
        assertThat(claims.get("id", Integer.class)).isEqualTo(7);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
        assertThat(jwtService.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    @DisplayName("UT-JWT-02 extrae el correo del token")
    void extractsEmail() {
        String token = jwtService.generateToken(user());

        assertThat(jwtService.extractEmail(token)).isEqualTo("ana@sigsu.local");
    }

    @Test
    @DisplayName("UT-JWT-03 rechaza un token expirado")
    void rejectsExpiredToken() {
        JwtService expiredService = new JwtService(new JwtProperties(SECRET, -60_000L));
        String token = expiredService.generateToken(user());

        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("UT-JWT-04 rechaza un token firmado con otra clave")
    void rejectsTokenSignedWithAnotherKey() {
        JwtService otherService = new JwtService(new JwtProperties(OTHER_SECRET, EXPIRATION_MS));
        String token = otherService.generateToken(user());

        assertThatThrownBy(() -> jwtService.extractEmail(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("UT-JWT-05 rechaza una cadena que no es un JWT")
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractEmail("esto-no-es-un-jwt"))
                .isInstanceOf(MalformedJwtException.class);
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key(SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    private UsuarioEntity user() {
        UsuarioEntity user = new UsuarioEntity();
        ReflectionTestUtils.setField(user, "id", 7);
        user.setNombres("Ana");
        user.setApellidos("Perez");
        user.setCorreo("ana@sigsu.local");
        user.setRol(RolEnum.SOLICITANTE);
        user.setActivo(true);
        return user;
    }
}
