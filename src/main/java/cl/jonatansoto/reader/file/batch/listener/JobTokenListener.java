package cl.jonatansoto.reader.file.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobTokenListener implements JobExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(JobTokenListener.class);
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        String token = jobExecution.getJobParameters().getString("token");
        logger.info("=== INICIO JOB - Execution ID: {}, Token: {} ===", jobExecution.getId(), token);
        jobExecution.getExecutionContext().put("token", token);
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        String token = jobExecution.getJobParameters().getString("token");
        logger.info("=== FIN JOB - Execution ID: {}, Token: {}, Status: {} ===", 
                jobExecution.getId(), token, jobExecution.getStatus());
    }
}

