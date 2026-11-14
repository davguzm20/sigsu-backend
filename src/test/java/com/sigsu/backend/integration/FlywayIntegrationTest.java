package com.sigsu.backend.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayIntegrationTest extends IntegrationTestBase {

    @Autowired
    private Flyway flyway;

    @Test
    @DisplayName("INT-11 Flyway aplica V1 a V3 sobre una base vacía")
    void appliesMigrationsOnAnEmptyDatabase() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(jdbcTemplate.queryForList("SHOW TABLES", String.class))
                .contains("areas", "categorias", "prioridades", "usuarios", "solicitudes", "asignaciones",
                        "seguimientos", "evidencias", "historial_estados");
    }
}
