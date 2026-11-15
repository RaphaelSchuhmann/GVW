package com.gvw.gvwbackend.dto.deploymentService.response;

import com.gvw.gvwbackend.model.deploymentSystem.HealthStatus;

public record HealthResponseDTO(HealthStatus app, HealthStatus database) {}
