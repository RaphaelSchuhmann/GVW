package com.gvw.gvwbackend.middleware;

import com.gvw.gvwbackend.state.MaintenanceState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that rejects all incoming requests with {@code 503 Service Unavailable} while the
 * application is in maintenance mode.
 */
@Component
public class MaintenanceInterceptor implements HandlerInterceptor {
  private final MaintenanceState state;

  public MaintenanceInterceptor(MaintenanceState state) {
    this.state = state;
  }

  /**
   * Checks the maintenance state before a request is handled.
   *
   * @param request the current HTTP request
   * @param response the current HTTP response
   * @param handler the chosen handler for the request
   * @return {@code true} if the request may proceed, {@code false} if maintenance mode is active
   * @throws Exception if an I/O error occurs while writing the error response
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (state.getActive()) {
      response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "0000503");
      return false;
    }

    return true;
  }
}
