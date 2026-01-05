package cl.jonatansoto.reader.file.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_procesados")
public class DocumentoProcesado {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long jobExecutionId;
    
    @Column(nullable = false)
    private String token;
    
    @Column(nullable = false)
    private String numeroOperacion;
    
    @Column(nullable = false)
    private String nombreArchivo;
    
    @Column(nullable = false, length = 500)
    private String pathCompleto;
    
    @Column(nullable = false, length = 20)
    private String estado; // PROCESADO, FALLIDO, PENDIENTE
    
    @Column(length = 2000)
    private String mensajeError;
    
    @Column(nullable = false)
    private LocalDateTime fechaProcesamiento;
    
    private Long tamañoArchivo;
    
    private Long tamañoBase64;
    
    private Integer estadoHttp; // 200, 400, 500, etc.

    // Constructors
    public DocumentoProcesado() {
    }

    public DocumentoProcesado(Long jobExecutionId, String token, String numeroOperacion, 
                              String nombreArchivo, String pathCompleto, String estado) {
        this.jobExecutionId = jobExecutionId;
        this.token = token;
        this.numeroOperacion = numeroOperacion;
        this.nombreArchivo = nombreArchivo;
        this.pathCompleto = pathCompleto;
        this.estado = estado;
        this.fechaProcesamiento = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public void setNumeroOperacion(String numeroOperacion) {
        this.numeroOperacion = numeroOperacion;
    }

    public String getNombreArchivo() {
        return nombreArchivo;
    }

    public void setNombreArchivo(String nombreArchivo) {
        this.nombreArchivo = nombreArchivo;
    }

    public String getPathCompleto() {
        return pathCompleto;
    }

    public void setPathCompleto(String pathCompleto) {
        this.pathCompleto = pathCompleto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public Long getTamañoArchivo() {
        return tamañoArchivo;
    }

    public void setTamañoArchivo(Long tamañoArchivo) {
        this.tamañoArchivo = tamañoArchivo;
    }

    public Long getTamañoBase64() {
        return tamañoBase64;
    }

    public void setTamañoBase64(Long tamañoBase64) {
        this.tamañoBase64 = tamañoBase64;
    }

    public Integer getEstadoHttp() {
        return estadoHttp;
    }

    public void setEstadoHttp(Integer estadoHttp) {
        this.estadoHttp = estadoHttp;
    }
}

