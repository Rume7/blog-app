package com.codehacks.blog.service;

import com.codehacks.blog.model.User;

public interface TokenService {

    /**
     * Generates a JWT token for the given user
     * @param user The user to generate token for
     * @return The generated JWT token
     */
    String generateToken(User user);

    /**
     * Validates a JWT token
     * @param token The token to validate
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Gets the user ID from a JWT token
     * @param token The token to extract user ID from
     * @return The user ID
     */
    Long getUserIdFromToken(String token);

    /**
     * Extracts the email from the JWT token.
     *
     * @param token the JWT token
     * @return the email claim embedded in the token
     */
    String getUserEmailFromToken(String token);

    /**
     * Invalidates a token (for logout)
     * @param token The token to invalidate
     */
    void invalidateToken(String token);

    /**
     * Check if redis has token associated with email
     * @param email email for token check
     * @return boolean True for when token exist.
     */
    boolean hasExistingToken(String email);

    /**
     * Get token from the user's email
     * @param email email for getting token value.
     */
    String getToken(String email);

}