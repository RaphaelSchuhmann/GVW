package com.gvw.gvwbackend.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class InternalAuthInterceptor implements HandlerInterceptor {
  @Value("${internal.api.token}")
  private String expectedToken;

  @jakarta.annotation.PostConstruct
  void validateToken() {
    if (expectedToken == null || expectedToken.isBlank()) {
      throw new IllegalStateException("internal.api.token must not be blank");
    }
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
