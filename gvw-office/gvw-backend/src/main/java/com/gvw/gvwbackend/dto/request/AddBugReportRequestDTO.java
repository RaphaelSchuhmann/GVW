package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddBugReportRequestDTO(
    @NotBlank String title,
    @NotBlank String severity,
    @NotBlank String stepsToReproduce,
    @NotBlank String route,
    @NotBlank String appVersion,
    @NotBlank String os,
    @NotBlank String browser,
    @NotBlank String viewport) {}
