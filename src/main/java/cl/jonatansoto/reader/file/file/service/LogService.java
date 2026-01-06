package cl.jonatansoto.reader.file.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public void sendLog(String nivel, String componente, String mensaje, Long jobExecutionId) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("fecha", LocalDateTime.now().format(FORMATTER));
        logEntry.put("nivel", nivel);
        logEntry.put("componente", componente);
        logEntry.put("mensaje", mensaje);
        logEntry.put("jobExecutionId", jobExecutionId);
        
        // Enviar a todos los clientes suscritos
        messagingTemplate.convertAndSend("/topic/logs", logEntry);
        
        // También enviar a un canal específico del job si existe
        if (jobExecutionId != null) {
            messagingTemplate.convertAndSend("/topic/logs/" + jobExecutionId, logEntry);
        }
    }
    
    public void sendInfo(String componente, String mensaje, Long jobExecutionId) {
        sendLog("INFO", componente, mensaje, jobExecutionId);
    }
    
    public void sendError(String componente, String mensaje, Long jobExecutionId) {
        sendLog("ERROR", componente, mensaje, jobExecutionId);
    }
    
    public void sendWarn(String componente, String mensaje, Long jobExecutionId) {
        sendLog("WARN", componente, mensaje, jobExecutionId);
    }
}

