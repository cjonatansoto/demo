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
    public synchronized OperacionDocumento read() throws Exception {
        Resource current = this.resource;
        if (current == null || !current.exists()) return null;

        String path = current.getFile().getAbsolutePath();
        String nroOp = extractNumeroOperacion(path);

        if (nroOp == null) {
            log.error("READER ERROR: No se encontró nro de operación en {}", path);
            this.resource = null;
            return null;
        }

        byte[] content = Files.readAllBytes(current.getFile().toPath());
        OperacionDocumento doc = new OperacionDocumento(nroOp, current.getFilename(), content, path);

        log.info("READER SUCCESS: Operación {} leída", nroOp);
        this.resource = null; // Liberar recurso para el siguiente
        return doc;
    }

    private String extractNumeroOperacion(String path) {
        if (path == null) return null;
        String clean = path.replace("\\", "/");
        return java.util.Arrays.stream(clean.split("/"))
                .filter(p -> p.matches("\\d{5,}")) // Busca el nro de 5 o más dígitos
                .findFirst().orElse(null);
    }

    @Override public synchronized void setResource(Resource resource) { this.resource = resource; }
    @Override public void open(ExecutionContext ec) {}
    @Override public void update(ExecutionContext ec) {}
    @Override public void close() {}
}