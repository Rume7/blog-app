package com.codehacks.blog.service;

import com.codehacks.blog.exception.TokenExpirationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public TokenService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean hasExistingToken(String username) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("token:" + username));
    }

    public String getExistingToken(String username) throws TokenExpirationException {
        if (hasExistingToken(username)) {
            return redisTemplate.opsForValue().get("token:" + username);
        }
        throw new TokenExpirationException("token is expired");
    }

    public void storeToken(String username, String token) {
        redisTemplate.opsForValue().set(
                "token:" + username,
                token,
                60,
                TimeUnit.MINUTES
        );
    }

    public boolean isTokenValid(String username, String token) {
        String storedToken = redisTemplate.opsForValue().get("token:" + username);
        return token.equals(storedToken);
    }

    public void invalidateToken(String username) {
        redisTemplate.delete("token:" + username);
    }
}
