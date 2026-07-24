package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record UpdateScoreRequestDTO(
    @NotBlank String id,
    @NotBlank String scoreId,
    @NotBlank String title,
    @NotBlank String artist,
    @NotBlank String type,
    @NotEmpty List<String> voices,
    @NotNull @Positive Integer voiceCount,
    @NotBlank String rev) {}
