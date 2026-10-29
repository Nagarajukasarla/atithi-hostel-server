package com.atithi.aavas.security;

import com.atithi.aavas.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final KeyLoader keyLoader;

    @Value("${jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(expirationSeconds);

        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("role", user.getRole() != null ? user.getRole().name() : null);
        claims.put("hostelId", user.getHostelId());

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(keyLoader.getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(keyLoader.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        Claims claims = extractClaims(token);
        return claims.get("email", String.class);
    }

    public Long extractUserId(String token) {
        Claims claims = extractClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public Long extractHostelId(String token) {
        Claims claims = extractClaims(token);
        Object hostelIdObj = claims.get("hostelId");
        if (hostelIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims.get("role", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
