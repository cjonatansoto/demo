package cl.jonatansoto.reader.file.batch.writer;

import cl.jonatansoto.reader.file.model.*;
import cl.jonatansoto.reader.file.repository.DocumentoProcesadoRepository;
import cl.jonatansoto.reader.file.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OperacionWriter implements ItemWriter<OperacionDocumento> {
    private final RestTemplate restTemplate;
    private final DocumentoProcesadoRepository documentoProcesadoRepository;
    private final TokenService tokenService;

    private Long jobId;
    private String token;

    @Value("${api.endpoint.base-url}") private String apiBaseUrl;

    @BeforeStep
    public void setup(StepExecution stepExecution) {
        // Esta inyección es segura por cada hilo
        this.jobId = stepExecution.getJobExecutionId();
        this.token = stepExecution.getExecutionContext().getString("token");
    }

    @Override
    @Transactional
    public void write(Chunk<? extends OperacionDocumento> chunk) {
        log.info("WRITER: Hilo {} procesando {} items", Thread.currentThread().getName(), chunk.size());
        for (OperacionDocumento doc : chunk) {
            process(doc);
        }
    }

    private void process(OperacionDocumento doc) {
        try {
            if (this.token == null || !tokenService.isValidJWT(this.token)) {
                save(doc, "FALLIDO", 401, "Token nulo o inválido");
                return;
            }

            String url = apiBaseUrl + "/" + doc.nroOperacion() + "/documents";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(this.token);

            DocumentoRequest req = DocumentoRequest.builder()
                    .fileBase64(doc.contenidoBase64()).fileName(doc.nombreArchivo()).build();

            ResponseEntity<SaveDocumentResponse> res = restTemplate.postForEntity(url, new HttpEntity<>(req, headers), SaveDocumentResponse.class);
            save(doc, "PROCESADO", res.getStatusCode().value(), null);

        } catch (Exception e) {
            log.error("WRITER ERROR: {} - {}", doc.nombreArchivo(), e.getMessage());
            save(doc, "FALLIDO", 0, e.getMessage());
        }
    }

    private void save(OperacionDocumento doc, String estado, Integer status, String err) {
        documentoProcesadoRepository.save(DocumentoProcesado.builder()
                .jobExecutionId(this.jobId).token(this.token).numeroOperacion(doc.nroOperacion())
                .nombreArchivo(doc.nombreArchivo()).pathCompleto(doc.pathCompleto())
                .estado(estado).mensajeError(err).estadoHttp(status)
                .fechaProcesamiento(LocalDateTime.now()).build());
    }
}