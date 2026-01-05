package cl.jonatansoto.reader.file.batch.reader;

import cl.jonatansoto.reader.file.model.OperacionDocumento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;
import java.nio.file.Files;

public class PdfItemReader implements ResourceAwareItemReaderItemStream<OperacionDocumento> {
    
    private static final Logger logger = LoggerFactory.getLogger(PdfItemReader.class);
    private Resource resource;

    @Override
    public OperacionDocumento read() throws Exception {
        if (resource == null) {
            logger.debug("READER: Recurso nulo, finalizando lectura");
            return null;
        }
        
        if (!resource.exists()) {
            logger.warn("READER: Recurso no existe: {}", resource.getFile().getAbsolutePath());
            this.resource = null;
            return null;
        }

        String absolutePath = resource.getFile().getAbsolutePath();
        logger.info("READER: Leyendo archivo: {}", absolutePath);
        
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
            logger.error("READER: No se puede extraer número de operación de la ruta: {}", absolutePath);
            this.resource = null;
            return null;
        }
        
        logger.info("READER: Número de operación extraído: {}", nroOperacion);

        byte[] content = Files.readAllBytes(resource.getFile().toPath());
        long tamañoArchivo = content.length;
        logger.info("READER: ✓ Archivo leído exitosamente - Operación: {}, Archivo: {}, Tamaño: {} bytes", 
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
                logger.debug("READER: Recurso asignado: {}", resource.getFile().getAbsolutePath());
            } catch (Exception e) {
                logger.debug("READER: Recurso asignado (no se pudo obtener path): {}", resource);
            }
        }
    }

    @Override public void open(ExecutionContext executionContext) {
        logger.info("READER: Abriendo reader");
    }
    @Override public void update(ExecutionContext executionContext) {}
    @Override public void close() {
        logger.info("READER: Cerrando reader");
    }
}