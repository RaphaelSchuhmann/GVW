package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddDeploymentRequestDTO;
import com.gvw.gvwbackend.dto.request.AddMigrationRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDeploymentInformationRequestDTO;
import com.gvw.gvwbackend.dto.response.DeploymentResponseDTO;
import com.gvw.gvwbackend.service.DeploymentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/deployment")
public class DeploymentController {
  private final DeploymentService deploymentService;

  public DeploymentController(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @GetMapping("/today")
  public Map<String, List<String>> getTodayDeployments() {
    List<String> times = deploymentService.getTodayDeployments();
    return Map.of("times", times);
  }

  @GetMapping("/all")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public List<DeploymentResponseDTO> getAllDeployments() {
    return deploymentService.getAllDeployments();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public DeploymentResponseDTO getDeployment(@PathVariable String id) {
    return deploymentService.getDeployment(id);
  }

  @GetMapping("/check/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void checkDeployment(@PathVariable String id) {
    deploymentService.checkDeployment(id);
  }

  @PostMapping("/add/deployment")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void addDeployment(@RequestBody @Valid AddDeploymentRequestDTO request) {
    deploymentService.addDeployment(request);
  }

  @PostMapping("/add/migration")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void addMigration(@RequestBody @Valid AddMigrationRequestDTO request) {
    deploymentService.addMigration(request);
  }

  @DeleteMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void removeDeployment(@PathVariable String id) {
    deploymentService.removeDeployment(id);
  }

  @DeleteMapping("/delete/{deployment}/{migrationId}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void removeMigration(@PathVariable String deploymentId, @PathVariable String migrationId) {
    deploymentService.removeMigration(deploymentId, migrationId);
  }

  @PatchMapping("/update")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void updateDeploymentInformation(
      @RequestBody @Valid UpdateDeploymentInformationRequestDTO request) {
    deploymentService.updateDeploymentInformation(request);
  }

  // TODO: Implement migration dry-run test endpoint when external migration handler is implemented
  // TODO: Implement deploy endpoint when external deployment handler is implemented
}
