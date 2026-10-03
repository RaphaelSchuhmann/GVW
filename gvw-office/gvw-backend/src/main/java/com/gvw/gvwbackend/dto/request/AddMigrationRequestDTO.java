package com.gvw.gvwbackend.dto.request;

import com.gvw.gvwbackend.model.Migration;
import com.gvw.gvwbackend.model.MigrationAction;
import jakarta.validation.constraints.NotNull;

public record AddMigrationRequestDTO(
    @NotNull String deploymentId,
    @NotNull String database,
    String field,
    @NotNull MigrationAction action,
    Migration.Conditional conditional,
    String value) {}
