package cl.jonatansoto.reader.file.batch.listener;

import cl.jonatansoto.reader.file.batch.writer.OperacionWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

@Component
public class CustomStepExecutionListener implements StepExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomStepExecutionListener.class);
    
    @Override
    public void beforeStep(StepExecution stepExecution) {
        ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
        String token = jobContext.getString("token");
        Long jobExecutionId = stepExecution.getJobExecution().getId();
        
        logger.info("=== INICIO STEP: {} - Job Execution ID: {}, Token: {} ===", 
                stepExecution.getStepName(), jobExecutionId, token);
        
        // Guardar token y jobExecutionId en ThreadLocal para que el writer pueda acceder
        OperacionWriter.setContext(token, jobExecutionId);
        
        // Guardar token y jobExecutionId en step context también
        ExecutionContext stepContext = stepExecution.getExecutionContext();
        stepContext.put("token", token);
        stepContext.put("jobExecutionId", jobExecutionId);
    }
    
    @Override
    public org.springframework.batch.core.ExitStatus afterStep(StepExecution stepExecution) {
        logger.info("=== FIN STEP: {} - Items leídos: {}, Items escritos: {}, Status: {} ===",
                stepExecution.getStepName(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getStatus());
        
        // Limpiar ThreadLocal
        OperacionWriter.clearContext();
        
        return stepExecution.getExitStatus();
    }
}

