package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AddChangelogRequestDTO(
    @NotBlank String version,
    @NotBlank String title,
    @NotBlank String content,
    @NotNull Instant timestamp) {}
