package cl.jonatansoto.reader.file.batch.processor;

import cl.jonatansoto.reader.file.model.OperacionDocumento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import java.util.Base64;

@Slf4j
@Component
public class OperacionProcessor implements ItemProcessor<OperacionDocumento, OperacionDocumento> {
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public OperacionDocumento process(OperacionDocumento item) {
        if (item.contenido() == null || item.contenido().length == 0) return null;

        String b64 = Base64.getEncoder().encodeToString(item.contenido());
        if (b64.length() > MAX_SIZE) {
            log.error("PROCESSOR: Archivo {} excede 10MB", item.nombreArchivo());
            return null;
        }

        return new OperacionDocumento(item.nroOperacion(), item.nombreArchivo(),
                item.contenido(), item.pathCompleto(), b64);
    }
}