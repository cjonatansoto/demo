package cl.jonatansoto.reader.file.batch.writer;

import cl.jonatansoto.reader.file.model.DocumentoProcesado;
import cl.jonatansoto.reader.file.model.DocumentoRequest;
import cl.jonatansoto.reader.file.model.OperacionDocumento;
import cl.jonatansoto.reader.file.repository.DocumentoProcesadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OperacionWriter implements ItemWriter<OperacionDocumento> {
    
    private static final Logger logger = LoggerFactory.getLogger(OperacionWriter.class);
    
    private static final ThreadLocal<String> tokenThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Long> jobExecutionIdThreadLocal = new ThreadLocal<>();
    
    private final RestTemplate restTemplate;
    private final String apiEndpointUrl;
    
    @Autowired
    private DocumentoProcesadoRepository documentoProcesadoRepository;
    
    public OperacionWriter(RestTemplate restTemplate, @Value("${api.endpoint.url}") String apiEndpointUrl) {
        this.restTemplate = restTemplate;
        this.apiEndpointUrl = apiEndpointUrl;
    }
    
    public static void setContext(String token, Long jobExecutionId) {
        tokenThreadLocal.set(token);
        jobExecutionIdThreadLocal.set(jobExecutionId);
    }
    
    public static void clearContext() {
        tokenThreadLocal.remove();
        jobExecutionIdThreadLocal.remove();
    }
    
    @Override
    @Transactional
    public void write(Chunk<? extends OperacionDocumento> chunk) {
        String token = tokenThreadLocal.get();
        Long jobExecutionId = jobExecutionIdThreadLocal.get();
        
        logger.info("=== WRITER: Procesando chunk de {} documentos - Token: {}, JobExecutionId: {} ===", 
                chunk.size(), token, jobExecutionId);
        
        for (OperacionDocumento doc : chunk) {
            DocumentoProcesado documentoProcesado = null;
            try {
                logger.debug("Procesando documento - Operación: {}, Archivo: {}", 
                        doc.nroOperacion(), doc.nombreArchivo());
                
                // Validar que el documento tenga contenido base64
                if (doc.contenidoBase64() == null || doc.contenidoBase64().isEmpty()) {
                    logger.error("Documento sin contenido base64 - Operación: {}, Archivo: {}", 
                    doc.nroOperacion(), doc.nombreArchivo());
                    
                    // Guardar como fallido
                    documentoProcesado = new DocumentoProcesado(
                            jobExecutionId, token, doc.nroOperacion(), 
                            doc.nombreArchivo(), doc.pathCompleto(), "FALLIDO");
                    documentoProcesado.setMensajeError("Documento sin contenido base64");
                    documentoProcesado.setTamañoArchivo((long) doc.contenido().length);
                    documentoProcesado.setEstadoHttp(0); // Sin estado HTTP
                    documentoProcesadoRepository.save(documentoProcesado);
                    continue;
                }
                
                // Crear request
                DocumentoRequest request = new DocumentoRequest(
                        doc.nroOperacion(),
                        doc.nombreArchivo(),
                        doc.contenidoBase64()
                );
                
                // Configurar headers con JWT
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(token); // Agregar JWT como Bearer token
                HttpEntity<DocumentoRequest> entity = new HttpEntity<>(request, headers);
                
                logger.debug("Enviando documento con JWT token al endpoint: {} - Operación: {}, Archivo: {}", 
                        apiEndpointUrl, doc.nroOperacion(), doc.nombreArchivo());
                
                // Enviar al endpoint
                ResponseEntity<String> response = restTemplate.postForEntity(
                        apiEndpointUrl, 
                        entity, 
                        String.class
                );
                
                // Validar respuesta
                int statusCode = response.getStatusCode().value();
                if (response.getStatusCode().is2xxSuccessful()) {
                    logger.info("✓ Documento registrado correctamente - Operación: {}, Archivo: {}, Status: {}", 
                            doc.nroOperacion(), doc.nombreArchivo(), statusCode);
                    
                    // Guardar como procesado exitosamente
                    documentoProcesado = new DocumentoProcesado(
                            jobExecutionId, token, doc.nroOperacion(), 
                            doc.nombreArchivo(), doc.pathCompleto(), "PROCESADO");
                    documentoProcesado.setTamañoArchivo((long) doc.contenido().length);
                    documentoProcesado.setTamañoBase64((long) doc.contenidoBase64().length());
                    documentoProcesado.setEstadoHttp(statusCode); // Guardar estado HTTP (ej: 200)
                    documentoProcesadoRepository.save(documentoProcesado);
                } else {
                    logger.error("✗ Error al registrar documento - Operación: {}, Archivo: {}, Status: {}", 
                            doc.nroOperacion(), doc.nombreArchivo(), statusCode);
                    
                    // Guardar como fallido
                    documentoProcesado = new DocumentoProcesado(
                            jobExecutionId, token, doc.nroOperacion(), 
                            doc.nombreArchivo(), doc.pathCompleto(), "FALLIDO");
                    documentoProcesado.setMensajeError("Error HTTP: " + statusCode);
                    documentoProcesado.setTamañoArchivo((long) doc.contenido().length);
                    documentoProcesado.setTamañoBase64((long) doc.contenidoBase64().length());
                    documentoProcesado.setEstadoHttp(statusCode); // Guardar estado HTTP de error
                    documentoProcesadoRepository.save(documentoProcesado);
                }
                
            } catch (RestClientException e) {
                logger.error("✗ Error al enviar documento al endpoint - Operación: {}, Archivo: {}, Error: {}", 
                        doc.nroOperacion(), doc.nombreArchivo(), e.getMessage(), e);
                
                // Guardar como fallido
                if (documentoProcesado == null) {
                    documentoProcesado = new DocumentoProcesado(
                            jobExecutionId, token, doc.nroOperacion(), 
                            doc.nombreArchivo(), doc.pathCompleto(), "FALLIDO");
                }
                    documentoProcesado.setMensajeError("Error inesperado: " + e.getMessage());
                    if (doc.contenido() != null) {
                        documentoProcesado.setTamañoArchivo((long) doc.contenido().length);
                    }
                    if (doc.contenidoBase64() != null) {
                        documentoProcesado.setTamañoBase64((long) doc.contenidoBase64().length());
                    }
                    documentoProcesado.setEstadoHttp(0); // Sin estado HTTP (error inesperado)
                    documentoProcesadoRepository.save(documentoProcesado);
                
            } catch (Exception e) {
                logger.error("✗ Error inesperado al procesar documento - Operación: {}, Archivo: {}, Error: {}", 
                        doc.nroOperacion(), doc.nombreArchivo(), e.getMessage(), e);
                
                // Guardar como fallido
                if (documentoProcesado == null) {
                    documentoProcesado = new DocumentoProcesado(
                            jobExecutionId, token, doc.nroOperacion(), 
                            doc.nombreArchivo(), doc.pathCompleto(), "FALLIDO");
                }
                documentoProcesado.setMensajeError("Error inesperado: " + e.getMessage());
                if (doc.contenido() != null) {
                    documentoProcesado.setTamañoArchivo((long) doc.contenido().length);
                }
                if (doc.contenidoBase64() != null) {
                    documentoProcesado.setTamañoBase64((long) doc.contenidoBase64().length());
                }
                documentoProcesadoRepository.save(documentoProcesado);
            }
        }
        
        logger.info("=== WRITER: Chunk procesado - {} documentos procesados ===", chunk.size());
    }
}
