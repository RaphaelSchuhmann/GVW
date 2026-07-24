package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddReportRequestDTO(
    @NotBlank String title,
    @NotBlank String author,
    @NotBlank String type,
    @NotBlank String description) {}
