package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record ChangePwRequestDTO(
    @NotNull @Email String email, @NotNull String oldPassword, @NotNull String newPassword) {}
