package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddCategoryRequestDTO(@NotBlank String type, @NotBlank String displayName) {}
