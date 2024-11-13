package com.codehacks.blog.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_SECRET_KEY = "thisIsATestSecretKeyThatIsLongEnoughForHS256";


    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", TEST_SECRET_KEY);
    }

    @Test
    void testGenerateToken() {
        // Given & When
        String token = jwtUtil.generateToken(TEST_USERNAME);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(token.split("\\.").length, 3); // Verify JWT structure
    }

    @Test
    void testExtractUsername() {
        // Given & When
        String token = jwtUtil.generateToken(TEST_USERNAME);
        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void testIsTokenExpired() {
        // Given: Generate a valid token
        String validToken = jwtUtil.generateToken(TEST_USERNAME);

        // When: Create an expired token directly
        Jwts.builder()
                .setSubject(TEST_USERNAME)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // 1 hour ago
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // expired 1 second ago
                .signWith(Keys.hmacShaKeyFor("your_super_secret_key_that_is_at_least_256_bits_long".getBytes()), SignatureAlgorithm.HS256)
                .compact();

        // Then
        assertFalse(jwtUtil.isTokenExpired(validToken), "The token should not be expired");
    }

    @Test
    void testInvalidTokenSignature() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0VXNlciJ9.invalidSignature";

        assertThrows(SignatureException.class, () ->
                jwtUtil.validateToken(invalidToken, TEST_USERNAME));
    }

    @Test
    void testMalformedToken() {
        String malformedToken = "not.a.jwt";

        assertThrows(MalformedJwtException.class, () ->
                jwtUtil.validateToken(malformedToken, TEST_USERNAME));
    }
}
