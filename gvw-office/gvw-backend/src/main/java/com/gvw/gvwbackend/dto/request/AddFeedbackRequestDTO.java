package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddFeedbackRequestDTO(
    @NotBlank String title,
    @NotBlank String category,
    String message,
    @NotNull @Positive Integer sentiment,
    @NotBlank String route,
    @NotBlank String appVersion) {}
