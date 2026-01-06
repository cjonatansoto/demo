package cl.jonatansoto.reader.file.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomStepExecutionListener implements StepExecutionListener {
    @Override
    public void beforeStep(StepExecution stepExecution) {
        String token = stepExecution.getJobExecution().getExecutionContext().getString("token");
        log.info("=== STEP START: {} | JobId: {} ===", stepExecution.getStepName(), stepExecution.getJobExecutionId());
        // Inyectar en el contexto del Step actual
        stepExecution.getExecutionContext().putString("token", token);
    }
}