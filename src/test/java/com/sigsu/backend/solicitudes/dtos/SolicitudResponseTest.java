package com.sigsu.backend.solicitudes.dtos;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.solicitudes.entities.EstadoSolicitudEnum;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudResponseTest {

    @Test
    void mapsTheDeadline() {
        LocalDateTime deadline = LocalDateTime.of(2026, 11, 20, 18, 0);

        SolicitudResponse response = SolicitudResponse.from(solicitud(deadline), null);

        assertThat(response.getFechaLimite()).isEqualTo(deadline);
    }

    @Test
    void keepsTheDeadlineNullWhenThereIsNoDeadline() {
        SolicitudResponse response = SolicitudResponse.from(solicitud(null), null);

        assertThat(response.getFechaLimite()).isNull();
    }

    private SolicitudEntity solicitud(LocalDateTime deadline) {
        UsuarioEntity solicitante = new UsuarioEntity();
        solicitante.setNombres("Ana");
        solicitante.setApellidos("Perez");

        SolicitudEntity solicitud = new SolicitudEntity();
        solicitud.setCodigo("SOL-2026-000001");
        solicitud.setSolicitante(solicitante);
        solicitud.setEstado(EstadoSolicitudEnum.EN_EVALUACION);
        solicitud.setTitulo("Falla de red");
        solicitud.setDescripcion("Sin conexión en el laboratorio");
        solicitud.setUbicacion("Laboratorio 3");
        solicitud.setFechaLimite(deadline);

        return solicitud;
    }
}
