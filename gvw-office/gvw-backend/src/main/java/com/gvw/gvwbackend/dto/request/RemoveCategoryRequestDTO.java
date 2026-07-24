package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotNull;

public record RemoveCategoryRequestDTO(@NotNull String type) {}
