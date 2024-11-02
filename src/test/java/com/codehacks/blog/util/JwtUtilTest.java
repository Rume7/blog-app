package com.codehacks.blog.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secretKey = "your_secret_key_which_is_at_least_256_bits_long";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
    }

    @Test
    void testGenerateToken() {
        // Given
        String username = "testUser";

        // When
        String token = jwtUtil.generateToken(username);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        // Given
        String username = "testUser";

        // When
        String token = jwtUtil.generateToken(username);
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void testIsTokenExpired() {
        // Given: Generate a valid token
        String validToken = jwtUtil.generateToken("testUser");

        // When: Create an expired token directly
        Jwts.builder()
                .setSubject("testUser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // expired 1 second ago
                .signWith(Keys.hmacShaKeyFor("your_super_secret_key_that_is_at_least_256_bits_long".getBytes()), SignatureAlgorithm.HS256)
                .compact();

        // Then
        assertFalse(jwtUtil.isTokenExpired(validToken), "The token should not be expired");
    }
}
