package cl.jonatansoto.reader.file.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/token")
public class TokenGeneratorController {
    
    private static final String SECRET_KEY = "MySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm";
    
    /**
     * Endpoint para generar un token JWT mock válido para testing
     * GET /api/token/generate
     */
    @GetMapping("/generate")
    public Map<String, Object> generateToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        
        // Token válido por 24 horas
        Date expiration = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
        
        String token = Jwts.builder()
                .subject("test-user")
                .issuedAt(new Date())
                .expiration(expiration)
                .signWith(key)
                .compact();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiration", expiration);
        response.put("message", "Token JWT generado exitosamente. Válido por 24 horas.");
        
        return response;
    }
}

