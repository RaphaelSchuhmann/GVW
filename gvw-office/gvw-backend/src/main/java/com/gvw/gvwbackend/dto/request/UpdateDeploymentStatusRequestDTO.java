package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateDeploymentStatusRequestDTO(
    @NotNull @NotBlank String status, @NotNull @PositiveOrZero Integer reason) {}
