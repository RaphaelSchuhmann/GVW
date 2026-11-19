package com.gvw.gvwbackend.configuration;

import com.gvw.gvwbackend.middleware.InternalAuthInterceptor;
import com.gvw.gvwbackend.middleware.MaintenanceInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC configuration registering the application's servlet interceptors. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final InternalAuthInterceptor internalAuthInterceptor;
  private final MaintenanceInterceptor maintenanceInterceptor;

  public WebMvcConfig(
      InternalAuthInterceptor internalAuthInterceptor,
      MaintenanceInterceptor maintenanceInterceptor) {
    this.internalAuthInterceptor = internalAuthInterceptor;
    this.maintenanceInterceptor = maintenanceInterceptor;
  }

  /**
   * Registers the internal auth interceptor for {@code /internal/**} paths and the maintenance
   * interceptor for all paths except {@code /internal/deployment/**}.
   *
   * @param registry the interceptor registry
   */
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(internalAuthInterceptor).addPathPatterns("/internal/**");

    registry
        .addInterceptor(maintenanceInterceptor)
        .addPathPatterns("/**")
        .excludePathPatterns("/internal/deployment/**");
  }
}
