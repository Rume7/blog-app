package com.codehacks.blog.service;

import com.codehacks.blog.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "token:";

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private Key signingKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = this.jwtSecret.getBytes();
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + this.jwtExpiration);

        String token = Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(this.signingKey, SignatureAlgorithm.HS512)
                .compact();

        // Store token in Redis with 6 hours expiration
        redisTemplate.opsForValue().set(
                KEY_PREFIX + user.getEmail(),
                token,
                6,
                TimeUnit.HOURS
        );
        return token;
    }

    @Override
    public String getToken(String email) {
        return redisTemplate.opsForValue().get(KEY_PREFIX + email);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(this.signingKey)
                    .build()
                    .parseClaimsJws(token);
            return isTokenValid(token);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(this.signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.parseLong(claims.getSubject());
    }

    @Override
    public String getUserEmailFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("email", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void invalidateToken(String email) {
        if (hasExistingToken(email)) {
            redisTemplate.delete(KEY_PREFIX + email);
        }
    }

    private boolean isTokenValid(String token) {
        String email = getUserEmailFromToken(token);
        if (email == null) {
            return false;
        }
        String storedToken = redisTemplate.opsForValue().get(KEY_PREFIX + email);
        return token.equals(storedToken);
    }

    @Override
    public boolean hasExistingToken(String email) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + email));
    }
}