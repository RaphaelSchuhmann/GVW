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

@Configuration
public class AppConfig {

  @Bean
  @Qualifier("dbRestTemplate")
  public RestTemplate dbRestTemplate(
      @Value("${couchdb.user}") String user, @Value("${couchdb.password}") String password) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(user, password));
    return restTemplate;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtService jwtService() {
    return new JwtService();
  }
}
