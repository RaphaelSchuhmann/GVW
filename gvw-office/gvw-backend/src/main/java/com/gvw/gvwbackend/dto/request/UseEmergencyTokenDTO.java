package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UseEmergencyTokenDTO(@NotBlank String token) {}
