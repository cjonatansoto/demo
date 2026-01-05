package cl.jonatansoto.reader.file.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);
    private static final String SECRET_KEY = "MySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm";
    
    /**
     * Valida si el token JWT es válido
     */
    public boolean isValidJWT(String token) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Token JWT es nulo o vacío");
            return false;
        }
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // Verificar que no esté expirado
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(Date.from(Instant.now()))) {
                logger.warn("Token JWT expirado: {}", token);
                return false;
            }
            
            logger.info("Token JWT válido - Expira: {}", expiration != null ? expiration : "Sin expiración");
            return true;
        } catch (Exception e) {
            logger.error("Error al validar token JWT: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene la fecha de expiración del token JWT
     */
    public Date getExpirationDate(String token) {
        if (!isValidJWT(token)) {
            return null;
        }
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error("Error al obtener fecha de expiración del token JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Formatea la fecha de expiración
     */
    public String getExpirationDateFormatted(String token) {
        Date expirationDate = getExpirationDate(token);
        if (expirationDate == null) {
            return "Token inválido o sin expiración";
        }
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(expirationDate);
    }
}
