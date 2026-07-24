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

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  private final AuthMiddleware authMiddleware;
  private final EmergencySecurityFilter emergencySecurityFilter;

  @Value("${cors.allowed-origins:http://localhost:5173}")
  private List<String> allowedOrigins;

  public SecurityConfig(
      AuthMiddleware authMiddleware, EmergencySecurityFilter emergencySecurityFilter) {
    this.authMiddleware = authMiddleware;
    this.emergencySecurityFilter = emergencySecurityFilter;
  }

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
                    .requestMatchers("/auth/login", "/auth/changePw", "/settings/get")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(emergencySecurityFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(authMiddleware, EmergencySecurityFilter.class);

    return http.build();
  }

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
