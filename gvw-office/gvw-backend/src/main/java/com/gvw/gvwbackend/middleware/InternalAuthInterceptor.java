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

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    String token = request.getHeader("X-Internal-Auth");
    if (expectedToken.equals(token)) {
      return true;
    }
    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
    return false;
  }
}
