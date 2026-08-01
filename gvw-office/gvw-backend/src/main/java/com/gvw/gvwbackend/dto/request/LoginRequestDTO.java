package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginRequestDTO(@NotNull @Size(max = 320) String email, @NotNull String password) {}
