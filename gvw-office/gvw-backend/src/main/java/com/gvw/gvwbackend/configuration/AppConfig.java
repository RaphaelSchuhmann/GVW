package com.gvw.gvwbackend.configuration;

import com.gvw.gvwbackend.service.JwtService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

/**
 * General Spring application bean configuration.
 *
 * <p>Provides shared application components such as:
 *
 * <ul>
 *   <li>Database HTTP client
 *   <li>Password hashing implementation
 *   <li>JWT service
 * </ul>
 */
@Configuration
public class AppConfig {

  /**
   * Creates the RestTemplate used for CouchDB communication.
   *
   * <p>The client automatically adds HTTP Basic Authentication credentials required by CouchDB.
   */
  @Bean
  @Qualifier("dbRestTemplate")
  public RestTemplate dbRestTemplate(
      @Value("${couchdb.user}") String user, @Value("${couchdb.password}") String password) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(user, password));
    return restTemplate;
  }

  // BCrypt is used for one-way password hashing
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtService jwtService() {
    return new JwtService();
  }
}
