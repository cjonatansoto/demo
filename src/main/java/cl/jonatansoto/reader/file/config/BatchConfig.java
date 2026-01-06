package cl.jonatansoto.reader.file.config;

import cl.jonatansoto.reader.file.batch.listener.CustomStepExecutionListener;
import cl.jonatansoto.reader.file.batch.listener.JobTokenListener;
import cl.jonatansoto.reader.file.batch.reader.PdfItemReader;
import cl.jonatansoto.reader.file.model.OperacionDocumento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    @Value("${path.base.operaciones}")
    private String pathBase;
    
    private final JobTokenListener jobTokenListener;
    
    private final CustomStepExecutionListener stepExecutionListener;

    @Bean
    public MultiResourceItemReader<OperacionDocumento> multiResourceReader() throws IOException {
        String pattern = "file:" + pathBase + "/**/*.pdf";
        log.info("=== CONFIG: Buscando archivos PDF en: {}", pattern);
        
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(pattern);
        log.info("=== CONFIG: Archivos PDF encontrados: {}", resources.length);
        
        for (Resource resource : resources) {
            log.info("=== CONFIG: Archivo encontrado: {}", resource.getFile().getAbsolutePath());
        }
        
        return new MultiResourceItemReaderBuilder<OperacionDocumento>()
                .name("multiPdfReader")
                .resources(resources)
                .delegate(new PdfItemReader())
                .build();
    }

    @Bean
    public Job procesarDocumentosJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("procesarDocumentosJob", jobRepository)
                .listener(jobTokenListener)
                .start(step1)
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(10); // Máximo 10 hilos paralelos
        return executor;
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      MultiResourceItemReader<OperacionDocumento> reader,
                      ItemProcessor<OperacionDocumento, OperacionDocumento> processor,
                      ItemWriter<OperacionDocumento> writer,
                      TaskExecutor taskExecutor) {
        return new StepBuilder("lecturaArchivosStep", jobRepository)
                .<OperacionDocumento, OperacionDocumento>chunk(5, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .taskExecutor(taskExecutor) // Procesamiento paralelo con hilos
                .listener(stepExecutionListener)
                .build();
    }
}