package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotNull;

public record LoginRequestDTO(@NotNull String email, @NotNull String password) {}
