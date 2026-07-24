package com.gvw.gvwbackend.middleware;

import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthMiddleware extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  private final List<String> EXCLUDED_PATHS =
      List.of("/auth/login", "/dev/**", "/emergency/**", "/settings/get", "/auth/changePw");

  public AuthMiddleware(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  private void sendUnauthorized(HttpServletResponse response) throws IOException {
    String code = String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401));
    response.setHeader("X-GVW-Error-Code", code);
    response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-GVW-Error-Code");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, code);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();

    return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws IOException, ServletException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendUnauthorized(response);
      return;
    }

    String token = authHeader.substring(7);

    try {
      String userId = jwtService.extractUserId(token);
      Claims claims = jwtService.extractAllClaims(token);

      String roleName = claims.get("role", String.class);

      if (roleName == null) {
        sendUnauthorized(response);
        return;
      }

      Role role = Role.fromString(roleName);

      List<SimpleGrantedAuthority> authorities =
          List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

      var authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);

      SecurityContextHolder.getContext().setAuthentication(authToken);

      request.setAttribute("userId", userId);
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      sendUnauthorized(response);
    }
  }
}
