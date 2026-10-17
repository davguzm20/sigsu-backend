package com.sigsu.backend.solicitudes.entities;

import com.sigsu.backend.auth.entities.UsuarioEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidencias")
@EntityListeners(AuditingEntityListener.class)
public class EvidenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudEntity solicitud;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seguimiento_id")
    private SeguimientoEntity seguimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cargado_por_id", nullable = false)
    private UsuarioEntity cargadoPor;

    @Column(name = "nombre_archivo", nullable = false, length = 250)
    private String nombreArchivo;

    @Column(name = "tipo_mime", length = 100)
    private String tipoMime;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "archivo_url", nullable = false, length = 1000)
    private String archivoUrl;

    @CreatedDate
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    public Integer getId() {
        return id;
    }

    public SolicitudEntity getSolicitud() {
        return solicitud;
    }

    public void setSolicitud(SolicitudEntity solicitud) {
        this.solicitud = solicitud;
    }

    public SeguimientoEntity getSeguimiento() {
        return seguimiento;
    }

    public void setSeguimiento(SeguimientoEntity seguimiento) {
        this.seguimiento = seguimiento;
    }

    public UsuarioEntity getCargadoPor() {
        return cargadoPor;
    }

    public void setCargadoPor(UsuarioEntity cargadoPor) {
        this.cargadoPor = cargadoPor;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getArchivoUrl() {
        return archivoUrl;
    }

    public void setArchivoUrl(String archivoUrl) {
        this.archivoUrl = archivoUrl;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
