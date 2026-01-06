package cl.jonatansoto.reader.file.file.batch.reader;

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
        // Verificar que el recurso no sea null
        if (resource == null) {
            log.debug("READER: Recurso nulo, finalizando lectura");
            return null;
        }
        
        // Verificar que el recurso existe antes de acceder a sus métodos
        if (!resource.exists()) {
            try {
                String resourcePath = resource.getURI() != null ? resource.getURI().toString() : resource.toString();
                log.warn("READER: Recurso no existe: {}", resourcePath);
            } catch (Exception e) {
                log.warn("READER: Recurso no existe y no se pudo obtener su ruta: {}", e.getMessage());
            }
            this.resource = null;
            return null;
        }

        // Obtener la ruta absoluta de forma segura
        String absolutePath;
        try {
            absolutePath = resource.getFile().getAbsolutePath();
        } catch (Exception e) {
            log.error("READER: Error al obtener ruta del archivo: {}", e.getMessage());
            this.resource = null;
            return null;
        }
        
        log.info("READER: Leyendo archivo: {}", absolutePath);
        
        // Extraer el número de operación que es el nombre de la carpeta (ej: .../dat/615731/1/ejemplo.pdf -> extrae 615731)
        String nroOperacion = extractNumeroOperacion(absolutePath);
        
        if (nroOperacion == null || nroOperacion.isEmpty()) {
            log.error("READER: No se puede extraer número de operación de la ruta: {}", absolutePath);
            log.error("READER: Partes de la ruta: {}", java.util.Arrays.toString(absolutePath.split("/")));
            this.resource = null;
            return null;
        }
        
        log.info("READER: Número de operación extraído: {}", nroOperacion);

        // Leer el contenido del archivo de forma segura
        byte[] content;
        String fileName;
        try {
            content = Files.readAllBytes(resource.getFile().toPath());
            fileName = resource.getFilename();
        } catch (Exception e) {
            log.error("READER: Error al leer archivo {}: {}", absolutePath, e.getMessage());
            this.resource = null;
            return null;
        }
        
        long tamañoArchivo = content.length;
        log.info("READER: ✓ Archivo leído exitosamente - Operación: {}, Archivo: {}, Tamaño: {} bytes", 
                nroOperacion, fileName, tamañoArchivo);

        OperacionDocumento doc = new OperacionDocumento(nroOperacion, fileName, content, absolutePath);

        // Importante: anular el recurso para que MultiResourceItemReader pase al siguiente
        // Esto debe hacerse DESPUÉS de crear el documento para evitar problemas de concurrencia
        this.resource = null;
        return doc;
    }
    
    @Override
    public void setResource(Resource resource) {
        this.resource = resource;
        if (resource != null) {
            try {
                if (resource.exists() && resource.getFile() != null) {
                    log.debug("READER: Recurso asignado: {}", resource.getFile().getAbsolutePath());
                } else {
                    log.debug("READER: Recurso asignado: {}", resource.getURI() != null ? resource.getURI().toString() : resource.toString());
                }
            } catch (Exception e) {
                log.debug("READER: Recurso asignado (no se pudo obtener path): {}", resource);
            }
        } else {
            log.debug("READER: Recurso establecido a null");
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
    
    /**
     * Extrae el número de operación de la ruta del archivo.
     * La estructura esperada es: .../dat/{numeroOperacion}/.../archivo.pdf
     * 
     * @param absolutePath Ruta absoluta del archivo
     * @return Número de operación o null si no se puede extraer
     */
    private String extractNumeroOperacion(String absolutePath) {
        if (absolutePath == null || absolutePath.isEmpty()) {
            log.warn("READER: Ruta absoluta es null o vacía");
            return null;
        }
        
        // Normalizar separadores de ruta (Windows usa \)
        String normalizedPath = absolutePath.replace("\\", "/");
        
        // Dividir la ruta en partes
        String[] parts = normalizedPath.split("/");
        
        if (parts.length < 2) {
            log.warn("READER: Ruta tiene muy pocas partes: {}", normalizedPath);
            return null;
        }
        
        // Buscar la carpeta después de "dat"
        for (int i = 0; i < parts.length - 1; i++) {
            if ("dat".equals(parts[i]) && i + 1 < parts.length) {
                String nroOperacion = parts[i + 1];
                // Validar que sea un número
                if (nroOperacion != null && !nroOperacion.isEmpty() && nroOperacion.matches("\\d+")) {
                    log.debug("READER: Número de operación encontrado en índice {}: {}", i + 1, nroOperacion);
                    return nroOperacion;
                } else {
                    log.warn("READER: El valor después de 'dat' no es un número válido: {}", nroOperacion);
                }
            }
        }
        
        // Si no se encontró "dat", intentar buscar un número que parezca ser el número de operación
        // Buscar el primer número de 6 dígitos (típico formato de número de operación)
        for (String part : parts) {
            if (part != null && part.matches("\\d{6,}")) {
                log.debug("READER: Número de operación encontrado como número largo: {}", part);
                return part;
            }
        }
        
        log.warn("READER: No se encontró número de operación válido en la ruta: {}", normalizedPath);
        return null;
    }
}