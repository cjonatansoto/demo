package cl.jonatansoto.reader.file.batch.reader;

import cl.jonatansoto.reader.file.model.OperacionDocumento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.core.io.Resource;

import java.nio.file.Files;
import java.util.Arrays;

@Slf4j
public class PdfItemReader implements ResourceAwareItemReaderItemStream<OperacionDocumento> {

    private Resource resource;

    @Override
    public synchronized OperacionDocumento read() throws Exception {
        Resource currentResource = this.resource;

        if (currentResource == null) return null;

        if (!currentResource.exists()) {
            this.resource = null;
            return null;
        }

        String absolutePath;
        try {
            // Usamos getURI para ser más flexibles con el tipo de recurso
            absolutePath = currentResource.getFile().getAbsolutePath();
        } catch (Exception e) {
            log.error("READER: No se pudo obtener la ruta del archivo: {}", e.getMessage());
            this.resource = null;
            return null;
        }

        // EXTRACCIÓN CON LOGS DE DEPURACIÓN
        String nroOperacion = extractNumeroOperacion(absolutePath);

        if (nroOperacion == null || nroOperacion.isEmpty()) {
            log.error("READER ERROR: Falló extracción de operación. Ruta recibida: [{}]", absolutePath);
            this.resource = null; // Detenemos este recurso para no entrar en bucle
            return null;
        }

        byte[] content;
        String fileName;
        try {
            content = Files.readAllBytes(currentResource.getFile().toPath());
            fileName = currentResource.getFilename();
        } catch (Exception e) {
            log.error("READER: Error de lectura física en {}: {}", absolutePath, e.getMessage());
            this.resource = null;
            return null;
        }

        log.info("READER SUCCESS: Operación {} | Archivo {}", nroOperacion, fileName);

        OperacionDocumento doc = new OperacionDocumento(nroOperacion, fileName, content, absolutePath);
        this.resource = null;
        return doc;
    }

    /**
     * Extrae el número de operación intentando múltiples patrones.
     */
    private String extractNumeroOperacion(String path) {
        if (path == null) return null;

        // 1. Normalizar la ruta para procesarla uniformemente
        String cleanPath = path.replace("\\", "/");
        String[] parts = cleanPath.split("/");

        log.debug("DEBUG: Analizando partes de ruta: {}", Arrays.toString(parts));

        // Estrategia A: Buscar el patrón "/dat/{nro}/"
        for (int i = 0; i < parts.length - 1; i++) {
            if ("dat".equalsIgnoreCase(parts[i])) {
                String potentialNro = parts[i + 1];
                if (isValidNro(potentialNro)) {
                    log.debug("DEBUG: Encontrado por patrón 'dat/{}'", potentialNro);
                    return potentialNro;
                }
            }
        }

        // Estrategia B: Buscar la carpeta raíz inmediatamente anterior a la subcarpeta final
        // Ejemplo: .../615731/1/archivo.pdf -> El número suele ser el que está antes de la carpeta '1' o '0'
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].matches("\\d{5,}") && (parts[i+1].equals("1") || parts[i+1].equals("0"))) {
                log.debug("DEBUG: Encontrado por jerarquía de carpetas: {}", parts[i]);
                return parts[i];
            }
        }

        // Estrategia C: El primer número largo que aparezca en la ruta (>= 5 dígitos)
        for (String part : parts) {
            if (isValidNro(part)) {
                log.debug("DEBUG: Encontrado primer número válido en ruta: {}", part);
                return part;
            }
        }

        return null;
    }

    private boolean isValidNro(String str) {
        // Valida que sea numérico y tenga al menos 5 dígitos (ajustar según tu negocio)
        return str != null && str.matches("\\d{5,}");
    }

    @Override
    public synchronized void setResource(Resource resource) {
        this.resource = resource;
    }

    @Override public void open(ExecutionContext ec) {}
    @Override public void update(ExecutionContext ec) {}
    @Override public void close() {}
}