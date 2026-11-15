package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.deploymentService.response.HealthResponseDTO;
import com.gvw.gvwbackend.model.Deployment;
import com.gvw.gvwbackend.model.deploymentSystem.HealthStatus;
import com.gvw.gvwbackend.service.DeploymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class InternalController {
  private final DeploymentService deploymentService;

  public InternalController(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @GetMapping("/health")
  public ResponseEntity<HealthResponseDTO> health() {
    HealthResponseDTO health = deploymentService.healthCheck();
    boolean isUnhealthy = health.app() == HealthStatus.UNHEALTHY || health.database() == HealthStatus.UNHEALTHY;

    HttpStatus status = isUnhealthy ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK;
    return ResponseEntity.status(status).body(health);
  }
}
