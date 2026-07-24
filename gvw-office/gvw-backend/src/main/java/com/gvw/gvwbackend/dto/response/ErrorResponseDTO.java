package com.gvw.gvwbackend.dto.response;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ErrorResponseDTO(@NotNull String message, @Nullable Map<String, Object> details) {}
