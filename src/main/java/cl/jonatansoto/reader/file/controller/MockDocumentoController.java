package cl.jonatansoto.reader.file.controller;

import cl.jonatansoto.reader.file.model.DocumentoRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documentos")
public class MockDocumentoController {
    
    private static final Logger logger = LoggerFactory.getLogger(MockDocumentoController.class);
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> recibirDocumento(
            @RequestBody DocumentoRequest request,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // Extraer JWT del header Authorization (Bearer token)
        String token = authHeader != null && authHeader.startsWith("Bearer ") 
                ? authHeader.substring(7) 
                : null;
        
        logger.info("=== MOCK ENDPOINT: Recibido documento - Operación: {}, Archivo: {}, Tamaño Base64: {} bytes, JWT: {} ===",
                request.numeroOperacion(), 
                request.nombreArchivo(),
                request.contenidoBase64() != null ? request.contenidoBase64().length() : 0,
                token != null ? "Presente" : "No presente");
        
        // Simular procesamiento
        try {
            Thread.sleep(100); // Simular delay de procesamiento
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Validar tamaño del base64 (simulando validación)
        if (request.contenidoBase64() != null && request.contenidoBase64().length() > 10 * 1024 * 1024) {
            logger.warn("✗ MOCK ENDPOINT: Documento rechazado por tamaño excedido - Operación: {}, Archivo: {}",
                    request.numeroOperacion(), request.nombreArchivo());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Tamaño de archivo excedido");
            errorResponse.put("numeroOperacion", request.numeroOperacion());
            errorResponse.put("nombreArchivo", request.nombreArchivo());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        logger.info("✓ MOCK ENDPOINT: Documento procesado exitosamente - Operación: {}, Archivo: {}",
                request.numeroOperacion(), request.nombreArchivo());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Documento registrado correctamente");
        response.put("numeroOperacion", request.numeroOperacion());
        response.put("nombreArchivo", request.nombreArchivo());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}

