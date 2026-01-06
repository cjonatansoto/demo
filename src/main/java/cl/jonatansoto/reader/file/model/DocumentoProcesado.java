package cl.jonatansoto.reader.file.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_procesados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    
    private Integer estadoHttp;
}
