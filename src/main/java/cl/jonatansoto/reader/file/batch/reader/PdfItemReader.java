package cl.jonatansoto.reader.file.batch.reader;

import cl.jonatansoto.reader.file.model.OperacionDocumento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import java.nio.file.Files;

@Slf4j
public class PdfItemReader implements ResourceAwareItemReaderItemStream<OperacionDocumento> {
    
    private Resource resource;

    @Override
    public OperacionDocumento read() throws Exception {
        if (resource == null) {
            log.debug("READER: Recurso nulo, finalizando lectura");
            return null;
        }
        
        if (!resource.exists()) {
            log.warn("READER: Recurso no existe: {}", resource.getFile().getAbsolutePath());
            this.resource = null;
            return null;
        }

        String absolutePath = resource.getFile().getAbsolutePath();
        log.info("READER: Leyendo archivo: {}", absolutePath);
        
        // Extraer el número de operación que es el nombre de la carpeta (ej: .../dat/615731/1/ejemplo.pdf -> extrae 615731)
        String[] parts = absolutePath.split("/");
        String nroOperacion = null;
        
        // Buscar la carpeta después de "dat"
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("dat") && i + 1 < parts.length) {
                nroOperacion = parts[i + 1];
                break;
            }
        }
        
        if (nroOperacion == null || nroOperacion.isEmpty()) {
            log.error("READER: No se puede extraer número de operación de la ruta: {}", absolutePath);
            this.resource = null;
            return null;
        }
        
        log.info("READER: Número de operación extraído: {}", nroOperacion);

        byte[] content = Files.readAllBytes(resource.getFile().toPath());
        long tamañoArchivo = content.length;
        log.info("READER: ✓ Archivo leído exitosamente - Operación: {}, Archivo: {}, Tamaño: {} bytes", 
                nroOperacion, resource.getFilename(), tamañoArchivo);

        OperacionDocumento doc = new OperacionDocumento(nroOperacion, resource.getFilename(), content, absolutePath);

        // Importante: anular el recurso para que MultiResourceItemReader pase al siguiente
        this.resource = null;
        return doc;
    }
    
    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        if (resource != null) {
            try {
                log.debug("READER: Recurso asignado: {}", resource.getFile().getAbsolutePath());
            } catch (Exception e) {
                log.debug("READER: Recurso asignado (no se pudo obtener path): {}", resource);
            }
        }
    }

    @Override 
    public void open(ExecutionContext executionContext) {
        log.info("READER: Abriendo reader");
    }
    
    @Override 
    public void update(ExecutionContext executionContext) {
        // No se requiere implementación
    }
    
    @Override 
    public void close() {
        log.info("READER: Cerrando reader");
    }
}