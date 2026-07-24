package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddMemberRequestDTO(
    @NotBlank String name,
    @NotBlank String surname,
    @NotBlank @Email String email,
    @NotBlank String phone,
    @NotBlank String address,
    @NotBlank String voice,
    @NotBlank String status,
    @NotBlank String role,
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z$",
            message = "Date must be a valid ISO-8601 UTC string (e.g., 2026-04-09T00:00:00.000Z)")
        @NotBlank
        String birthdate,
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?Z$",
            message = "Date must be a valid ISO-8601 UTC string (e.g., 2026-04-09T00:00:00.000Z)")
        @NotBlank
        String joined) {}
