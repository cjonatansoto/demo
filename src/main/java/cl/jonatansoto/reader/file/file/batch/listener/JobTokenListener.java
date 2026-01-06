package cl.jonatansoto.reader.file.file.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JobTokenListener implements JobExecutionListener {
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String token = jobExecution.getJobParameters().getString("token");
        log.info("=== INICIO JOB - Execution ID: {}, Token: {} ===", jobExecution.getId(), token);
        jobExecution.getExecutionContext().put("token", token);
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        String token = jobExecution.getJobParameters().getString("token");
        log.info("=== FIN JOB - Execution ID: {}, Token: {}, Status: {} ===", 
                jobExecution.getId(), token, jobExecution.getStatus());
    }
}

