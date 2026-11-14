package com.sigsu.backend.integration;

import com.sigsu.backend.asignaciones.entities.AsignacionEntity;
import com.sigsu.backend.asignaciones.repositories.AsignacionRepository;
import com.sigsu.backend.auth.entities.RolEnum;
import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.auth.repositories.UsuarioRepository;
import com.sigsu.backend.catalogos.entities.AreaEntity;
import com.sigsu.backend.catalogos.entities.CategoriaEntity;
import com.sigsu.backend.catalogos.entities.PrioridadEntity;
import com.sigsu.backend.catalogos.repositories.AreaRepository;
import com.sigsu.backend.catalogos.repositories.CategoriaRepository;
import com.sigsu.backend.catalogos.repositories.PrioridadRepository;
import com.sigsu.backend.common.security.JwtService;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import com.sigsu.backend.solicitudes.repositories.EvidenciaRepository;
import com.sigsu.backend.solicitudes.repositories.SolicitudRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestContainersConfig.class)
abstract class IntegrationTestBase {

    private static final AtomicInteger CODES = new AtomicInteger(100_000);
    private static final AtomicInteger LEVELS = new AtomicInteger(10);

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected MySQLContainer mysqlContainer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected UsuarioRepository usuarioRepository;

    @Autowired
    protected AreaRepository areaRepository;

    @Autowired
    protected CategoriaRepository categoriaRepository;

    @Autowired
    protected PrioridadRepository prioridadRepository;

    @Autowired
    protected SolicitudRepository solicitudRepository;

    @Autowired
    protected AsignacionRepository asignacionRepository;

    @Autowired
    protected EvidenciaRepository evidenciaRepository;

    @BeforeEach
    void verifiesTestDatabase() {
        assertThat(jdbcTemplate.queryForObject("SELECT DATABASE()", String.class)).isEqualTo(mysqlContainer.getDatabaseName());
    }

    protected String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected UsuarioEntity createUser(String prefix, RolEnum rol) {
        UsuarioEntity user = new UsuarioEntity();
        user.setNombres("Nombre " + prefix);
        user.setApellidos("Apellido");
        user.setCorreo(unique(prefix) + "@sigsu.local");
        user.setPasswordHash("$2a$10$hash");
        user.setRol(rol);
        user.setActivo(true);
        return usuarioRepository.save(user);
    }

    protected AreaEntity createArea() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AreaEntity area = new AreaEntity();
        area.setCodigo("AREA-" + suffix);
        area.setNombre("Área " + suffix);
        return areaRepository.save(area);
    }

    protected CategoriaEntity createCategoria() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        CategoriaEntity categoria = new CategoriaEntity();
        categoria.setCodigo("CAT-" + suffix);
        categoria.setNombre("Categoría " + suffix);
        return categoriaRepository.save(categoria);
    }

    protected PrioridadEntity createPrioridad() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        PrioridadEntity prioridad = new PrioridadEntity();
        prioridad.setCodigo("PRI-" + suffix);
        prioridad.setNombre("Prioridad " + suffix);
        prioridad.setNivel((byte) LEVELS.incrementAndGet());
        return prioridadRepository.save(prioridad);
    }

    protected SolicitudEntity createSolicitud(UsuarioEntity solicitante, EstadoSolicitudEnum estado) {
        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setCodigo(String.format("SOL-2026-%06d", CODES.incrementAndGet()));
        solicitud.setSolicitante(solicitante);
        solicitud.setEstado(estado);
        solicitud.setTitulo("Solicitud de prueba");
        solicitud.setDescripcion("Descripción de prueba");
        solicitud.setUbicacion("Laboratorio 3");
        return solicitudRepository.save(solicitud);
    }

    protected AsignacionEntity assign(SolicitudEntity solicitud, UsuarioEntity tecnico, UsuarioEntity supervisor,
                                      LocalDateTime finalizadaEn) {
        AsignacionEntity asignacion = new AsignacionEntity();
        asignacion.setSolicitud(solicitud);
        asignacion.setTecnico(tecnico);
        asignacion.setAsignadoPor(supervisor);
        asignacion.setAsignadaEn(LocalDateTime.now().minusDays(1));
        asignacion.setFinalizadaEn(finalizadaEn);
        return asignacionRepository.save(asignacion);
    }

    protected String tokenFor(UsuarioEntity user) {
        return jwtService.generateToken(user);
    }

    protected String bearer(UsuarioEntity user) {
        return "Bearer " + tokenFor(user);
    }

    protected String json(Object value) {
        return objectMapper.writeValueAsString(value);
    }

    protected void setCreatedAt(SolicitudEntity solicitud, LocalDateTime date) {
        jdbcTemplate.update("UPDATE solicitudes SET creado_en = ? WHERE id = ?",
                Timestamp.valueOf(date), solicitud.getId());
        entityManager.flush();
        entityManager.clear();
    }

}
