package com.gvw.gvwbackend.configuration;

import com.gvw.gvwbackend.middleware.AuthMiddleware;
import com.gvw.gvwbackend.middleware.EmergencySecurityFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security configuration for the application.
 *
 * <p>The application uses stateless JWT-based authentication instead of server-side sessions.
 * Incoming requests are authenticated through the custom {@link AuthMiddleware}.
 *
 * <p>The security chain performs the following:
 *
 * <ul>
 *   <li>Configures CORS handling
 *   <li>Disables CSRF because the application does not use cookie-based sessions
 *   <li>Enforces stateless session management
 *   <li>Allows unauthenticated access to public endpoints
 *   <li>Requires authentication for all remaining endpoints
 * </ul>
 *
 * <p>The {@link EmergencySecurityFilter} is placed before authentication to protect emergency
 * endpoints that intentionally bypass normal JWT validation.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final AuthMiddleware authMiddleware;
  private final EmergencySecurityFilter emergencySecurityFilter;

  @Value("${cors.allowed-origins}")
  private List<String> allowedOrigins;

  public SecurityConfig(
      AuthMiddleware authMiddleware, EmergencySecurityFilter emergencySecurityFilter) {
    this.authMiddleware = authMiddleware;
    this.emergencySecurityFilter = emergencySecurityFilter;
  }

  /**
   * Configures the application's HTTP security filter chain.
   *
   * <p>Requests are authenticated using JWT tokens through {@link AuthMiddleware}. CORS preflight
   * requests and explicitly configured public endpoints are allowed without authentication.
   *
   * @param http Spring Security HTTP configuration
   * @return configured security filter chain
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exc ->
                exc.authenticationEntryPoint(
                    (request, response, authException) -> {
                      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest)
                    .permitAll()
                    .requestMatchers(
                        "/auth/login", "/auth/changePw", "/settings/get", "/sync/stream")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(emergencySecurityFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(authMiddleware, EmergencySecurityFilter.class);

    return http.build();
  }

  /**
   * Configures CORS rules for frontend communication.
   *
   * <p>The allowed origins are loaded from configuration to support different deployment
   * environments. The API exposes selected headers required by the frontend.
   *
   * @return CORS configuration source
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("Content-Disposition"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
