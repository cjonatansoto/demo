package cl.jonatansoto.reader.file.file.batch.writer;

import cl.jonatansoto.reader.file.model.DocumentoProcesado;
import cl.jonatansoto.reader.file.model.DocumentoRequest;
import cl.jonatansoto.reader.file.model.OperacionDocumento;
import cl.jonatansoto.reader.file.model.SaveDocumentResponse;
import cl.jonatansoto.reader.file.repository.DocumentoProcesadoRepository;
import cl.jonatansoto.reader.file.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperacionWriter implements ItemWriter<OperacionDocumento> {
    
    private static final ThreadLocal<String> tokenThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Long> jobExecutionIdThreadLocal = new ThreadLocal<>();
    
    private final RestTemplate restTemplate;
    private final DocumentoProcesadoRepository documentoProcesadoRepository;
    private final TokenService tokenService;
    
    @Value("${api.endpoint.base-url}")
    private String apiBaseUrl;
    
    @Value("${api.document.class}")
    private String documentClass;
    
    @Value("${api.document.type}")
    private String documentType;
    
    @Value("${api.document.client-id}")
    private String clientId;
    
    @Value("${api.document.document-server}")
    private String documentServer;
    
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
        
        log.info("=== WRITER: Procesando chunk de {} documentos - JobExecutionId: {} ===", 
                chunk.size(), jobExecutionId);
        
        for (OperacionDocumento doc : chunk) {
            processDocument(doc, token, jobExecutionId);
        }
        
        log.info("=== WRITER: Chunk procesado - {} documentos procesados ===", chunk.size());
    }
    
    private void processDocument(OperacionDocumento doc, String token, Long jobExecutionId) {
        try {
            log.debug("Procesando documento - Operación: {}, Archivo: {}", 
                    doc.nroOperacion(), doc.nombreArchivo());
            
            // Verificar si el documento ya fue procesado exitosamente
            DocumentoProcesado documentoExistente = documentoProcesadoRepository
                    .findByNumeroOperacionAndNombreArchivoAndEstado(
                            doc.nroOperacion(), 
                            doc.nombreArchivo(), 
                            "PROCESADO"
                    );
            
            if (documentoExistente != null) {
                log.info("⏭️  Documento ya procesado exitosamente - Operación: {}, Archivo: {} - Omitiendo procesamiento", 
                        doc.nroOperacion(), doc.nombreArchivo());
                return; // Omitir procesamiento
            }
            
            // Validar token antes de procesar
            if (!tokenService.isValidJWT(token)) {
                log.error("✗ Token inválido, corrupto o expirado - Operación: {}, Archivo: {}", 
                        doc.nroOperacion(), doc.nombreArchivo());
                
                saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                        401, "Token inválido, corrupto o expirado");
                
                // Lanzar excepción para detener el procesamiento
                throw new TokenExpiredException("Token inválido, corrupto o expirado");
            }
            
            // Validar que el documento tenga contenido base64
            if (doc.contenidoBase64() == null || doc.contenidoBase64().isEmpty()) {
                log.error("Documento sin contenido base64 - Operación: {}, Archivo: {}", 
                        doc.nroOperacion(), doc.nombreArchivo());
                
                saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                        0, "Documento sin contenido base64");
                return;
            }
            
            // Construir URL dinámica con operationId
            String endpointUrl = buildEndpointUrl(doc.nroOperacion());
            
            // Crear request con valores del application.yaml
            DocumentoRequest request = DocumentoRequest.builder()
                    .fileBase64(doc.contenidoBase64())
                    .documentClass(documentClass)
                    .documentType(documentType)
                    .fileName(doc.nombreArchivo())
                    .clientId(clientId)
                    .documentServer(documentServer)
                    .build();
            
            // Configurar headers con JWT
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);
            HttpEntity<DocumentoRequest> entity = new HttpEntity<>(request, headers);
            
            log.debug("Enviando documento al endpoint: {} - Operación: {}, Archivo: {}", 
                    endpointUrl, doc.nroOperacion(), doc.nombreArchivo());
            
            // Enviar al endpoint
            ResponseEntity<SaveDocumentResponse> response = restTemplate.postForEntity(
                    endpointUrl,
                    entity,
                    SaveDocumentResponse.class
            );
            
            // Validar respuesta
            int statusCode = response.getStatusCode().value();
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                SaveDocumentResponse responseBody = response.getBody();
                log.info("✓ Documento registrado correctamente - Operación: {}, Archivo: {}, Status: {}, GNID: {}, OperationId: {}", 
                        doc.nroOperacion(), doc.nombreArchivo(), statusCode, 
                        responseBody.getGnid(), responseBody.getOperationId());
                
                saveDocumentoProcesado(doc, token, jobExecutionId, "PROCESADO", 
                        statusCode, null, responseBody);
            } else {
                log.error("✗ Error al registrar documento - Operación: {}, Archivo: {}, Status: {}", 
                        doc.nroOperacion(), doc.nombreArchivo(), statusCode);
                
                saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                        statusCode, "Error HTTP: " + statusCode);
            }
            
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("✗ Token expirado o inválido (401) - Operación: {}, Archivo: {}", 
                        doc.nroOperacion(), doc.nombreArchivo());
                
                // Guardar el documento como fallido pero sin estado unauthorized para poder retomar
                saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                        401, "Token expirado o inválido - Se guardó para retomar");
                
                // Lanzar excepción personalizada para detener el procesamiento
                throw new TokenExpiredException("Token expirado o inválido", e);
            } else {
                log.error("✗ Error HTTP al enviar documento - Operación: {}, Archivo: {}, Status: {}", 
                        doc.nroOperacion(), doc.nombreArchivo(), e.getStatusCode());
                
                saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                        e.getStatusCode().value(), "Error HTTP: " + e.getMessage());
            }
        } catch (TokenExpiredException e) {
            // Re-lanzar la excepción para detener el procesamiento
            throw e;
        } catch (RestClientException e) {
            log.error("✗ Error al enviar documento al endpoint - Operación: {}, Archivo: {}, Error: {}", 
                    doc.nroOperacion(), doc.nombreArchivo(), e.getMessage(), e);
            
            saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                    0, "Error de conexión: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("✗ Error inesperado al procesar documento - Operación: {}, Archivo: {}, Error: {}", 
                    doc.nroOperacion(), doc.nombreArchivo(), e.getMessage(), e);
            
            saveDocumentoProcesado(doc, token, jobExecutionId, "FALLIDO", 
                    0, "Error inesperado: " + e.getMessage());
        }
    }
    
    private String buildEndpointUrl(String operationId) {
        // Construir URL automáticamente: base-url/operationId/documents
        return String.format("%s/%s/documents", apiBaseUrl, operationId);
    }
    
    private void saveDocumentoProcesado(OperacionDocumento doc, String token, Long jobExecutionId,
                                       String estado, Integer estadoHttp, String mensajeError) {
        saveDocumentoProcesado(doc, token, jobExecutionId, estado, estadoHttp, mensajeError, null);
    }
    
    private void saveDocumentoProcesado(OperacionDocumento doc, String token, Long jobExecutionId,
                                       String estado, Integer estadoHttp, String mensajeError, 
                                       SaveDocumentResponse response) {
        DocumentoProcesado documentoProcesado = DocumentoProcesado.builder()
                .jobExecutionId(jobExecutionId)
                .token(token)
                .numeroOperacion(doc.nroOperacion())
                .nombreArchivo(doc.nombreArchivo())
                .pathCompleto(doc.pathCompleto())
                .estado(estado)
                .mensajeError(mensajeError)
                .fechaProcesamiento(LocalDateTime.now())
                .tamañoArchivo(doc.contenido() != null ? (long) doc.contenido().length : 0L)
                .tamañoBase64(doc.contenidoBase64() != null ? (long) doc.contenidoBase64().length() : 0L)
                .estadoHttp(estadoHttp)
                .build();
        
        documentoProcesadoRepository.save(documentoProcesado);
        log.debug("Documento guardado - Operación: {}, Archivo: {}, Estado: {}", 
                doc.nroOperacion(), doc.nombreArchivo(), estado);
    }
    
    /**
     * Excepción personalizada para manejar tokens expirados
     */
    public static class TokenExpiredException extends RuntimeException {
        public TokenExpiredException(String message) {
            super(message);
        }
        
        public TokenExpiredException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
