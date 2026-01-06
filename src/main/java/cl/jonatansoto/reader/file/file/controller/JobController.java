package cl.jonatansoto.reader.file.file.controller;

import cl.jonatansoto.reader.file.model.DocumentoProcesado;
import cl.jonatansoto.reader.file.repository.DocumentoProcesadoRepository;
import cl.jonatansoto.reader.file.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class JobController {

    private final JobLauncher jobLauncher;
    
    private final JobExplorer jobExplorer;
    
    private final DocumentoProcesadoRepository documentoProcesadoRepository;
    
    private final TokenService tokenService;
    
    @org.springframework.beans.factory.annotation.Autowired
    @Qualifier("procesarDocumentosJob")
    private Job procesarDocumentosJob;

    @GetMapping("/")
    public String index(Model model) {
        List<JobInstance> jobInstances = jobExplorer.getJobInstances("procesarDocumentosJob", 0, 100);
        List<Map<String, Object>> jobsInfo = new ArrayList<>();

        for (JobInstance jobInstance : jobInstances) {
            List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
            if (!jobExecutions.isEmpty()) {
                JobExecution latestExecution = jobExecutions.get(0);
                Map<String, Object> jobInfo = new HashMap<>();
                jobInfo.put("instanceId", jobInstance.getInstanceId());
                jobInfo.put("executionId", latestExecution.getId());
                jobInfo.put("status", latestExecution.getStatus().toString());
                jobInfo.put("startTime", latestExecution.getStartTime());
                jobInfo.put("endTime", latestExecution.getEndTime());
                jobInfo.put("exitStatus", latestExecution.getExitStatus().getExitCode());
                
                // Contar items procesados
                long itemsRead = 0;
                long itemsWritten = 0;
                for (StepExecution stepExecution : latestExecution.getStepExecutions()) {
                    itemsRead += stepExecution.getReadCount();
                    itemsWritten += stepExecution.getWriteCount();
                }
                jobInfo.put("itemsRead", itemsRead);
                jobInfo.put("itemsWritten", itemsWritten);
                
                // Obtener token del job
                String token = latestExecution.getJobParameters().getString("token");
                jobInfo.put("token", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "N/A");
                
                jobsInfo.add(jobInfo);
            }
        }

        // Ordenar por executionId descendente (más recientes primero)
        jobsInfo.sort((a, b) -> Long.compare((Long) b.get("executionId"), (Long) a.get("executionId")));

        model.addAttribute("jobs", jobsInfo);
        return "index";
    }

    @GetMapping("/job/{executionId}")
    public String jobDetails(@PathVariable Long executionId, Model model, 
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String message,
                            @org.springframework.web.bind.annotation.RequestParam(required = false) String error) {
        JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
        if (jobExecution == null) {
            return "redirect:/";
        }

        // Obtener documentos procesados para este job
        List<DocumentoProcesado> documentos = documentoProcesadoRepository.findByJobExecutionId(executionId);
        
        // Obtener token
        String token = jobExecution.getJobParameters().getString("token");
        
        // Agregar mensajes si existen
        if (message != null && !message.isEmpty()) {
            model.addAttribute("message", message);
        }
        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }

        // Construir logs de ejecución desde los StepExecutions
        List<Map<String, Object>> logs = new ArrayList<>();
        
        if (jobExecution.getStartTime() != null) {
            Map<String, Object> inicioLog = new HashMap<>();
            inicioLog.put("fecha", jobExecution.getStartTime());
            inicioLog.put("nivel", "INFO");
            inicioLog.put("componente", "JOB");
            inicioLog.put("mensaje", "Job iniciado - Execution ID: " + jobExecution.getId());
            logs.add(inicioLog);
        }
        
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStartTime() != null) {
                Map<String, Object> stepLog = new HashMap<>();
                stepLog.put("fecha", stepExecution.getStartTime());
                stepLog.put("nivel", "INFO");
                stepLog.put("componente", "STEP");
                stepLog.put("mensaje", String.format("Step '%s' iniciado - Items leídos: %d, Items escritos: %d", 
                        stepExecution.getStepName(), stepExecution.getReadCount(), stepExecution.getWriteCount()));
                logs.add(stepLog);
            }
            
            if (stepExecution.getFailureExceptions() != null && !stepExecution.getFailureExceptions().isEmpty()) {
                for (Throwable exception : stepExecution.getFailureExceptions()) {
                    Map<String, Object> errorLog = new HashMap<>();
                    errorLog.put("fecha", stepExecution.getEndTime() != null ? stepExecution.getEndTime() : stepExecution.getStartTime());
                    errorLog.put("nivel", "ERROR");
                    errorLog.put("componente", "STEP");
                    errorLog.put("mensaje", String.format("Error en step '%s': %s", 
                            stepExecution.getStepName(), exception.getMessage()));
                    logs.add(errorLog);
                }
            }
            
            if (stepExecution.getEndTime() != null) {
                Map<String, Object> stepFinLog = new HashMap<>();
                stepFinLog.put("fecha", stepExecution.getEndTime());
                stepFinLog.put("nivel", stepExecution.getStatus().toString().equals("COMPLETED") ? "INFO" : "ERROR");
                stepFinLog.put("componente", "STEP");
                stepFinLog.put("mensaje", String.format("Step '%s' finalizado - Status: %s", 
                        stepExecution.getStepName(), stepExecution.getStatus()));
                logs.add(stepFinLog);
            }
        }
        
        if (jobExecution.getEndTime() != null) {
            Map<String, Object> finLog = new HashMap<>();
            finLog.put("fecha", jobExecution.getEndTime());
            finLog.put("nivel", jobExecution.getStatus().toString().equals("COMPLETED") ? "INFO" : "ERROR");
            finLog.put("componente", "JOB");
            finLog.put("mensaje", String.format("Job finalizado - Status: %s, Exit Code: %s", 
                    jobExecution.getStatus(), jobExecution.getExitStatus().getExitCode()));
            logs.add(finLog);
        }

        model.addAttribute("jobExecution", jobExecution);
        model.addAttribute("stepExecutions", jobExecution.getStepExecutions());
        model.addAttribute("documentos", documentos);
        model.addAttribute("token", token);
        model.addAttribute("logs", logs);
        return "job-details";
    }

    @PostMapping("/job/start")
    public String startJob(@org.springframework.web.bind.annotation.RequestParam(value = "token", required = true) String token,
                          RedirectAttributes redirectAttributes) {
        try {
            if (token == null || token.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", 
                        "Token JWT es requerido.");
                return "redirect:/";
            }
            
            // Validar token antes de iniciar el job
            if (!tokenService.isValidJWT(token)) {
                redirectAttributes.addFlashAttribute("error", 
                        "Token JWT inválido, corrupto o expirado.");
                return "redirect:/";
            }
            
            // Obtener mensaje de expiración
            String expirationMessage = tokenService.getExpirationMessage(token);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("token", token)
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(procesarDocumentosJob, jobParameters);
            
            // Redirigir inmediatamente a la pantalla de ejecución para ver el proceso en tiempo real
            String message = "Job iniciado correctamente. " + expirationMessage;
            return "redirect:/job/" + jobExecution.getId() + "?message=" + 
                   java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("Error: El job ya se está ejecutando", e);
            redirectAttributes.addFlashAttribute("error", 
                    "El job ya se está ejecutando");
        } catch (JobRestartException e) {
            log.error("Error al reiniciar el job", e);
            redirectAttributes.addFlashAttribute("error", 
                    "Error al reiniciar el job: " + e.getMessage());
        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("Error: El job ya se completó anteriormente", e);
            redirectAttributes.addFlashAttribute("error", 
                    "El job ya se completó anteriormente");
        } catch (Exception e) {
            log.error("Error al iniciar el job", e);
            redirectAttributes.addFlashAttribute("error", 
                    "Error al iniciar el job: " + e.getMessage());
        }
        return "redirect:/";
    }
    
    @PostMapping("/job/delete/{executionId}")
    public String deleteJob(@PathVariable Long executionId, RedirectAttributes redirectAttributes) {
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
            if (jobExecution == null) {
                redirectAttributes.addFlashAttribute("error", 
                        "Job no encontrado. ID: " + executionId);
                return "redirect:/";
            }
            
            // Verificar que el job no esté en ejecución
            if (jobExecution.getStatus().isRunning()) {
                redirectAttributes.addFlashAttribute("error", 
                        "No se puede eliminar un job que está en ejecución. ID: " + executionId);
                return "redirect:/";
            }
            
            // Eliminar documentos relacionados primero
            List<DocumentoProcesado> documentos = documentoProcesadoRepository.findByJobExecutionId(executionId);
            if (!documentos.isEmpty()) {
                documentoProcesadoRepository.deleteAll(documentos);
                log.info("Eliminados {} documentos relacionados al job execution ID: {}", documentos.size(), executionId);
            }
            
            // Nota: Spring Batch no permite eliminar JobExecutions directamente desde la API
            // Los JobExecutions se mantienen en la base de datos para auditoría
            // Solo podemos eliminar los documentos relacionados
            
            redirectAttributes.addFlashAttribute("message", 
                    "Documentos relacionados eliminados correctamente. ID: " + executionId + 
                    " (Se eliminaron " + documentos.size() + " documentos). " +
                    "Nota: El registro del job se mantiene para auditoría.");
        } catch (Exception e) {
            log.error("Error al eliminar el job execution ID: {}", executionId, e);
            redirectAttributes.addFlashAttribute("error", 
                    "Error al eliminar el job: " + e.getMessage());
        }
        return "redirect:/";
    }
}
