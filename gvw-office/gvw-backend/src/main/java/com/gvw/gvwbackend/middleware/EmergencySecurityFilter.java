package com.gvw.gvwbackend.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class EmergencySecurityFilter extends OncePerRequestFilter {
  private final Set<String> LOCAL_IPS = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
  private static final Logger log = LoggerFactory.getLogger(EmergencySecurityFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getServletPath();

    if (path.startsWith("/emergency")) {
      String remoteAddr = request.getRemoteAddr();

      if (!LOCAL_IPS.contains(remoteAddr)) {
        log.warn("Unauthorized external access attempt to emergency endpoint.");
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Localhost only");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }
}
