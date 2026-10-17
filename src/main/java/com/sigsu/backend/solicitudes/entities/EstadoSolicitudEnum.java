package com.sigsu.backend.solicitudes.entities;

public enum EstadoSolicitudEnum {

    REGISTRADA,
    EN_EVALUACION,
    ASIGNADA,
    EN_ATENCION,
    RESUELTA,
    CERRADA;

    public boolean isFinal() {
        return this == CERRADA;
    }
}
