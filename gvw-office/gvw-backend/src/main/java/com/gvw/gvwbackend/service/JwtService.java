package com.gvw.gvwbackend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;

/**
 * Service responsible for creating and parsing JSON Web Tokens (JWT).
 *
 * <p>JWTs are used to authenticate users between requests. This service creates signed tokens
 * containing the user identifier and optional additional claims, and provides helper methods to
 * extract information from existing tokens.
 */
public class JwtService {
  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration-days}")
  private long expirationDays;

  private SecretKey key;

  /**
   * Initializes the cryptographic signing key used for JWT operations.
   *
   * <p>The key is created from the configured secret after Spring has injected all required
   * configuration values.
   *
   * @throws IllegalArgumentException if the configured secret is too short for the selected signing
   *     algorithm
   */
  @PostConstruct
  protected void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  /**
   * Generates a signed JWT for a user.
   *
   * <p>The token contains:
   *
   * <ul>
   *   <li>The user ID as the JWT subject
   *   <li>Additional custom claims provided by the caller
   *   <li>Creation timestamp
   *   <li>Expiration timestamp based on the configured lifetime
   * </ul>
   *
   * <p>The generated token is signed using the configured secret key, allowing later verification
   * that the token was created by this backend and has not been modified.
   *
   * @param userId identifier of the authenticated user
   * @param extraClaims additional data to include inside the token
   * @return signed JWT string
   */
  public String generateToken(String userId, Map<String, Object> extraClaims) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(userId)
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(expirationDays, ChronoUnit.DAYS)))
        .signWith(key)
        .compact();
  }

  /**
   * Extracts all claims stored inside a JWT.
   *
   * <p>The token signature is verified before returning the claims. Invalid or modified tokens will
   * cause parsing to fail.
   *
   * @param token JWT string to parse
   * @return all claims contained in the token
   */
  public Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  /**
   * Extracts the user identifier from a JWT.
   *
   * <p>The user ID is stored as the token subject during token generation.
   *
   * @param token JWT string
   * @return user ID stored inside the token
   */
  public String extractUserId(String token) {
    return extractAllClaims(token).getSubject();
  }
}
