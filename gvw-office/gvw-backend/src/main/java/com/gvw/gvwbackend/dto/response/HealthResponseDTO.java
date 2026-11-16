package com.gvw.gvwbackend.dto.response;

import com.gvw.gvwbackend.model.deploymentSystem.HealthStatus;

public record HealthResponseDTO(HealthStatus app, HealthStatus database) {}
