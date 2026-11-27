package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record UpdateDeploymentInformationRequestDTO(
    @NotNull String id,
    @NotNull String rev,
    @NotNull String title,
    @NotNull String version,
    @NotNull String date,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    String commit) {}
