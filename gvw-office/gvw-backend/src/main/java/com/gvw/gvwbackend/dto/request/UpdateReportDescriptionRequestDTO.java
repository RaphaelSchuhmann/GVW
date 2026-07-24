package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateReportDescriptionRequestDTO(
    @NotBlank String id, @NotBlank String rev, @NotNull String description) {}
