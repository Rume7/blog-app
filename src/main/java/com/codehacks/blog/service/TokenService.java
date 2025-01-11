package com.codehacks.blog.service;

import com.codehacks.blog.exception.TokenExpirationException;
import com.codehacks.blog.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "token:";
    private JwtUtil jwtUtil;

    @Autowired
    public TokenService(RedisTemplate<String, String> redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    public boolean hasExistingToken(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + username));
    }

    public String getExistingToken(String username) throws TokenExpirationException {
        if (hasExistingToken(username)) {
            return redisTemplate.opsForValue().get(KEY_PREFIX + username);
        }
        throw new TokenExpirationException("token is expired");
    }

    public void storeToken(String username, String token) {
        redisTemplate.opsForValue().set(KEY_PREFIX + username, token,
                60, TimeUnit.MINUTES);
    }

    public boolean isTokenValid(String username, String token) {
        if (!jwtUtil.validateToken(username, token)) {
            return false;
        }
        String storedToken = redisTemplate.opsForValue().get(KEY_PREFIX + username);
        return token.equals(storedToken);
    }

    public void invalidateToken(String username) {
        redisTemplate.delete(KEY_PREFIX + username);
    }
}
