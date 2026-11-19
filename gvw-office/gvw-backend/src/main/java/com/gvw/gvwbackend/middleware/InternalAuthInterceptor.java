package com.gvw.gvwbackend.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that authenticates internal service-to-service requests via a shared token sent in
 * the {@code X-Internal-Auth} header.
 */
@Component
public class InternalAuthInterceptor implements HandlerInterceptor {
  @Value("${internal.api.token}")
  private String expectedToken;

  /**
   * Validates that the configured internal API token is present and not blank.
   *
   * @throws IllegalStateException if {@code internal.api.token} is missing or blank
   */
  @jakarta.annotation.PostConstruct
  void validateToken() {
    if (expectedToken == null || expectedToken.isBlank()) {
      throw new IllegalStateException("internal.api.token must not be blank");
    }
  }

  /**
   * Compares the {@code X-Internal-Auth} request header against the configured token using a
   * constant-time comparison.
   *
   * @param request the current HTTP request
   * @param response the current HTTP response
   * @param handler the chosen handler for the request
   * @return {@code true} if the token matches, {@code false} otherwise
   * @throws Exception if an I/O error occurs while writing the error response
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    String token = request.getHeader("X-Internal-Auth");
    if (token != null
        && java.security.MessageDigest.isEqual(
            expectedToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
      return true;
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
    return false;
  }
}
