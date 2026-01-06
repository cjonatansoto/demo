package cl.jonatansoto.reader.file.file.repository;

import cl.jonatansoto.reader.file.model.DocumentoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoProcesadoRepository extends JpaRepository<DocumentoProcesado, Long> {
    
    List<DocumentoProcesado> findByJobExecutionId(Long jobExecutionId);
    
    List<DocumentoProcesado> findByToken(String token);
    
    List<DocumentoProcesado> findByTokenOrderByFechaProcesamientoAsc(String token);
    
    /**
     * Busca un documento procesado exitosamente por número de operación y nombre de archivo
     * @param numeroOperacion Número de operación
     * @param nombreArchivo Nombre del archivo
     * @return Documento procesado con estado PROCESADO, o null si no existe
     */
    DocumentoProcesado findByNumeroOperacionAndNombreArchivoAndEstado(
            String numeroOperacion, 
            String nombreArchivo, 
            String estado
    );
}

