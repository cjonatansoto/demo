package cl.jonatansoto.reader.file.repository;

import cl.jonatansoto.reader.file.model.DocumentoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoProcesadoRepository extends JpaRepository<DocumentoProcesado, Long> {
    
    List<DocumentoProcesado> findByJobExecutionId(Long jobExecutionId);
    
    List<DocumentoProcesado> findByToken(String token);
    
    List<DocumentoProcesado> findByTokenOrderByFechaProcesamientoAsc(String token);
}

