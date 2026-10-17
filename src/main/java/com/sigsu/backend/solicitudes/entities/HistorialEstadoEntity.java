package com.sigsu.backend.solicitudes.entities;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estados")
public class HistorialEstadoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudEntity solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_origen", length = 20)
    private EstadoSolicitudEnum estadoOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_destino", nullable = false, length = 20)
    private EstadoSolicitudEnum estadoDestino;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cambiado_por_id", nullable = false)
    private UsuarioEntity cambiadoPor;

    @Column(name = "cambiado_en", nullable = false)
    private LocalDateTime cambiadoEn;

    @Column(name = "observacion", length = 250)
    private String observacion;

    public Integer getId() {
        return id;
    }

    public SolicitudEntity getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudEntity solicitud) {
        this.solicitud = solicitud;
    }

    public EstadoSolicitudEnum getEstadoOrigen() {
        return estadoOrigen;
    }

    public void setEstadoOrigen(EstadoSolicitudEnum estadoOrigen) {
        this.estadoOrigen = estadoOrigen;
    }

    public EstadoSolicitudEnum getEstadoDestino() {
        return estadoDestino;
    }

    public void setEstadoDestino(EstadoSolicitudEnum estadoDestino) {
        this.estadoDestino = estadoDestino;
    }

    public UsuarioEntity getCambiadoPor() {
        return cambiadoPor;
    }

    public void setCambiadoPor(UsuarioEntity cambiadoPor) {
        this.cambiadoPor = cambiadoPor;
    }

    public LocalDateTime getCambiadoEn() {
        return cambiadoEn;
    }

    public void setCambiadoEn(LocalDateTime cambiadoEn) {
        this.cambiadoEn = cambiadoEn;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
