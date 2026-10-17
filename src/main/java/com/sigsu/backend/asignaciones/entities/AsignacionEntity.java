package com.sigsu.backend.asignaciones.entities;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import com.sigsu.backend.solicitudes.entities.SolicitudEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "asignaciones")
public class AsignacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tecnico_id", nullable = false)
    private UsuarioEntity tecnico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignado_por_id", nullable = false)
    private UsuarioEntity asignadoPor;

    @Column(name = "asignada_en", nullable = false)
    private LocalDateTime asignadaEn;

    @Column(name = "finalizada_en")
    private LocalDateTime finalizadaEn;

    public Integer getId() {
        return id;
    }

    public SolicitudEntity getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudEntity solicitud) {
        this.solicitud = solicitud;
    }

    public UsuarioEntity getTecnico() {
        return tecnico;
    }

    public void setTecnico(UsuarioEntity tecnico) {
        this.tecnico = tecnico;
    }

    public UsuarioEntity getAsignadoPor() {
        return asignadoPor;
    }

    public void setAsignadoPor(UsuarioEntity asignadoPor) {
        this.asignadoPor = asignadoPor;
    }

    public LocalDateTime getAsignadaEn() {
        return asignadaEn;
    }

    public void setAsignadaEn(LocalDateTime asignadaEn) {
        this.asignadaEn = asignadaEn;
    }

    public LocalDateTime getFinalizadaEn() {
        return finalizadaEn;
    }

    public void setFinalizadaEn(LocalDateTime finalizadaEn) {
        this.finalizadaEn = finalizadaEn;
    }
}
