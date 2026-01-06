package cl.jonatansoto.reader.file.batch.processor;

import cl.jonatansoto.reader.file.model.OperacionDocumento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class OperacionProcessor implements ItemProcessor<OperacionDocumento, OperacionDocumento> {
    
    private static final long MAX_SIZE_BASE64_MB = 10 * 1024 * 1024; // 10MB en bytes
    
    @Override
    public OperacionDocumento process(OperacionDocumento item) {
        log.debug("PROCESSOR: Iniciando procesamiento - Operación: {}, Archivo: {}", 
                item.nroOperacion(), item.nombreArchivo());
        
        // Validar que el archivo no esté vacío
        if (item.contenido().length == 0) {
            log.warn("✗ PROCESSOR: Archivo vacío rechazado - Operación: {}, Archivo: {}", 
                    item.nroOperacion(), item.nombreArchivo());
            return null;
        }
        
        log.debug("PROCESSOR: Convirtiendo a Base64 - Operación: {}, Archivo: {}, Tamaño original: {} bytes", 
                item.nroOperacion(), item.nombreArchivo(), item.contenido().length);
        
        // Convertir a base64
        String contenidoBase64 = Base64.getEncoder().encodeToString(item.contenido());
        
        // Validar tamaño del base64 (10MB máximo)
        long tamañoBase64 = contenidoBase64.length();
        if (tamañoBase64 > MAX_SIZE_BASE64_MB) {
            log.error("✗ PROCESSOR: Archivo excede el tamaño máximo de 10MB en base64 - Operación: {}, Archivo: {}, Tamaño Base64: {} bytes (máximo: {} bytes)", 
                    item.nroOperacion(), item.nombreArchivo(), tamañoBase64, MAX_SIZE_BASE64_MB);
            return null;
        }
        
        log.info("✓ PROCESSOR: Archivo procesado correctamente - Operación: {}, Archivo: {}, Tamaño original: {} bytes, Tamaño Base64: {} bytes", 
                item.nroOperacion(), item.nombreArchivo(), item.contenido().length, tamañoBase64);
        
        // Retornar nuevo documento con base64
        return new OperacionDocumento(
                item.nroOperacion(),
                item.nombreArchivo(),
                item.contenido(),
                item.pathCompleto(),
                contenidoBase64
        );
    }
}
