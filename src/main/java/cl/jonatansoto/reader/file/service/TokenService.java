package cl.jonatansoto.reader.file.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
public class TokenService {
    
    @Value("${jwt.secret-key:MySecretKeyForJWTTokenGenerationThatIsAtLeast256BitsLongForHS256Algorithm}")
    private String secretKey;
    
    /**
     * Valida si el token JWT es válido (no corrupto y no expirado)
     */
    public boolean isValidJWT(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token JWT es nulo o vacío");
            return false;
        }
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // Verificar que no esté expirado
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(Date.from(Instant.now()))) {
                log.warn("Token JWT expirado");
                return false;
            }
            
            log.debug("Token JWT válido - Expira: {}", expiration != null ? expiration : "Sin expiración");
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token JWT corrupto o mal formado: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Token JWT con firma inválida: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error al validar token JWT: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene la fecha de expiración del token JWT
     */
    public Date getExpirationDate(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Error al obtener fecha de expiración del token JWT: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Obtiene los minutos restantes hasta que expire el token
     */
    public long getMinutesUntilExpiration(String token) {
        Date expirationDate = getExpirationDate(token);
        if (expirationDate == null) {
            return -1;
        }
        
        Instant expirationInstant = expirationDate.toInstant();
        Instant now = Instant.now();
        
        if (expirationInstant.isBefore(now)) {
            return 0;
        }
        
        Duration duration = Duration.between(now, expirationInstant);
        return duration.toMinutes();
    }
    
    /**
     * Formatea la fecha de expiración
     */
    public String getExpirationDateFormatted(String token) {
        Date expirationDate = getExpirationDate(token);
        if (expirationDate == null) {
            return "Token inválido o sin expiración";
        }
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(expirationDate.toInstant().atZone(java.time.ZoneId.systemDefault()));
    }
    
    /**
     * Obtiene mensaje de expiración con minutos
     */
    public String getExpirationMessage(String token) {
        long minutes = getMinutesUntilExpiration(token);
        if (minutes < 0) {
            return "Token inválido";
        }
        if (minutes == 0) {
            return "Token expirado";
        }
        return String.format("Este token expira en %d minutos", minutes);
    }
}
