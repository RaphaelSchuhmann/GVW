package com.gvw.gvwbackend.dto.deploymentService.request;

import com.gvw.gvwbackend.model.Migration;
import java.util.List;

public record TriggerMigrationDryRunRequestDTO(List<Migration> migrations) {}
