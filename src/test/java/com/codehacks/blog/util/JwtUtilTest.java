package com.codehacks.blog.util;

import io.jsonwebtoken.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_SECRET_KEY = "thisIsATestSecretKeyThatIsLongEnoughForHS256";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3600000L); // 1 hour in milliseconds
        ReflectionTestUtils.setField(jwtUtil, "SECRET_KEY", TEST_SECRET_KEY);
    }

    @Test
    void testGenerateToken() {
        // Given & When
        String token = jwtUtil.generateToken(TEST_USERNAME);
        final int tokenStructureSize = 3;

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(token.split("\\.").length, tokenStructureSize); // Verify JWT structure
    }

    @Test
    void testExtractUsername() {
        // Given & When
        String token = jwtUtil.generateToken(TEST_USERNAME);

        assertNotNull(token);
        assertFalse(jwtUtil.isTokenExpired(token), "Token should not be expired.");

        String extractedUsername = jwtUtil.extractUsername(token);

        // Then
        assertEquals(TEST_USERNAME, extractedUsername);
    }

    @Test
    void testIsTokenExpired() {
        // Given: Generate a valid token
        String validToken = jwtUtil.generateToken(TEST_USERNAME);

        assertFalse(jwtUtil.isTokenExpired(validToken), "The valid token should not be expired");

        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 1800000L);
        String expiredToken = jwtUtil.generateToken(TEST_USERNAME);

        assertFalse(jwtUtil.isTokenExpired(expiredToken), "The token should be expired");

        // Reset the expiration time for other tests
        ReflectionTestUtils.setField(jwtUtil, "expirationTime", 3600000L);

        // Given: An invalid token
        String invalidToken = "invalid.token.string";

        // Then
        assertThrows(IllegalArgumentException.class, () ->
                        jwtUtil.isTokenExpired(invalidToken),
                "Should throw IllegalArgumentException for expired token");
    }

    @Test
    void testInvalidTokenSignature() {
        String invalidToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0VXNlciJ9.invalidSignature";

        assertThrows(JwtException.class, () ->
                jwtUtil.validateToken(invalidToken, TEST_USERNAME));
    }

    @Test
    void testMalformedToken() {
        String malformedToken = "not.a.jwt";

        assertThrows(JwtException.class, () ->
                jwtUtil.validateToken(malformedToken, TEST_USERNAME));
    }
}
