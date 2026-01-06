package cl.jonatansoto.reader.file.file.controller;

import cl.jonatansoto.reader.file.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {
    
    private final TokenService tokenService;
    
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        Map<String, Object> response = new HashMap<>();
        
        boolean isValid = tokenService.isValidJWT(token);
        response.put("valid", isValid);
        
        if (isValid) {
            String expirationMessage = tokenService.getExpirationMessage(token);
            response.put("message", expirationMessage);
            long minutes = tokenService.getMinutesUntilExpiration(token);
            response.put("minutesUntilExpiration", minutes);
        } else {
            response.put("message", "Token inválido, corrupto o expirado");
        }
        
        return ResponseEntity.ok(response);
    }
}

