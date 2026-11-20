package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.UpdateDeploymentStatusRequestDTO;
import com.gvw.gvwbackend.dto.response.HealthResponseDTO;
import com.gvw.gvwbackend.model.deploymentSystem.HealthStatus;
import com.gvw.gvwbackend.service.DeploymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    boolean isUnhealthy =
        health.app() == HealthStatus.UNHEALTHY || health.database() == HealthStatus.UNHEALTHY;

    HttpStatus status = isUnhealthy ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK;
    return ResponseEntity.status(status).body(health);
  }

  @PatchMapping("/deployment/{deploymentId}/status")
  @ResponseStatus(HttpStatus.OK)
  public void updateDeploymentStatus(
      @RequestBody @Valid UpdateDeploymentStatusRequestDTO request,
      @PathVariable String deploymentId) {
    deploymentService.updateDeploymentStatus(deploymentId, request);
  }
}
