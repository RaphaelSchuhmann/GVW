package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddHelpCenterCategoryRequestDTO(
    @NotBlank String title, @NotBlank String icon, @NotBlank String description) {}
