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

public class JwtService {
  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration-days}")
  private long expirationDays;

  private SecretKey key;

  @PostConstruct
  protected void init() {
    this.key = Keys.hmacShaKeyFor(secret.getBytes());
  }

  public String generateToken(String userId, Map<String, Object> extraClaims) {
    return Jwts.builder()
        .claims(extraClaims)
        .subject(userId)
        .issuedAt(new Date())
        .expiration(Date.from(Instant.now().plus(expirationDays, ChronoUnit.DAYS)))
        .signWith(key)
        .compact();
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public String extractUserId(String token) {
    return extractAllClaims(token).getSubject();
  }
}
