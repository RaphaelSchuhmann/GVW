package com.gvw.gvwbackend.middleware;

import static net.logstash.logback.argument.StructuredArguments.kv;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLogging extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(RequestLogging.class);

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long duration = System.currentTimeMillis() - start;

      log.info(
          "http request {} {} {} {}",
          kv("method", request.getMethod()),
          kv("endpoint", request.getRequestURI()),
          kv("status", response.getStatus()),
          kv("durationMs", duration));
    }
  }
}
