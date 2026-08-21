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

/**
 * Authentication middleware responsible for validating JWT bearer tokens and establishing the
 * Spring Security context.
 *
 * <p>For protected endpoints, the filter:
 *
 * <ul>
 *   <li>Extracts the JWT from the Authorization header
 *   <li>Validates the token using {@link JwtService}
 *   <li>Extracts the user ID and role claims
 *   <li>Creates an authenticated Spring Security context
 * </ul>
 *
 * <p>The following paths bypass authentication:
 *
 * <ul>
 *   <li>Authentication endpoints
 *   <li>Development endpoints
 *   <li>Emergency recovery endpoints
 *   <li>Public settings access
 *   <li>Password change endpoint
 * </ul>
 *
 * <p>Invalid or missing authentication information results in HTTP 401.
 */
@Component
public class AuthMiddleware extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  private final List<String> EXCLUDED_PATHS =
      List.of("/auth/login", "/dev/**", "/emergency/new", "/emergency/use", "/settings/get", "/auth/changePw");

  public AuthMiddleware(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  /**
   * Sends a standardized unauthorized response containing the GVW error code used by the frontend
   * to handle authentication failures.
   */
  private void sendUnauthorized(HttpServletResponse response) throws IOException {
    String code = String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401));
    response.setHeader("X-GVW-Error-Code", code);
    response.addHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-GVW-Error-Code");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, code);
  }

  /**
   * Determines whether authentication should be skipped for a request.
   *
   * <p>Paths matching the excluded endpoint list are allowed to continue without requiring a JWT.
   */
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();

    return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
  }

  /**
   * Processes incoming requests and authenticates users using JWT bearer tokens.
   *
   * <p>Requests using HTTP OPTIONS are passed through without authentication to allow CORS
   * preflight requests to succeed.
   *
   * <p>For all other requests, the filter:
   *
   * <ul>
   *   <li>Extracts the JWT from the Authorization header
   *   <li>Validates and parses the token using {@link JwtService}
   *   <li>Extracts the user ID and role from the token claims
   *   <li>Creates a Spring Security authentication context
   *   <li>Stores the authenticated user ID as a request attribute
   * </ul>
   *
   * <p>If the token is missing, invalid, or does not contain a valid role, the request is rejected
   * with HTTP 401 Unauthorized.
   *
   * @param request current HTTP request
   * @param response current HTTP response
   * @param filterChain chain used to continue request processing
   * @throws IOException if writing the response fails
   * @throws ServletException if request processing fails
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws IOException, ServletException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      // Allow CORS preflight requests without requiring authentication
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

      // Make user ID available for controllers/services without re-parsing the JWT
      request.setAttribute("userId", userId);
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      // Any JWT parsing, validation, or claim extraction failure is treated as invalid
      // authentication
      sendUnauthorized(response);
    }
  }
}
