package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserAdminRequestDTO(
    @NotBlank String id,
    @NotBlank String rev,
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank String phone,
    @NotBlank String address,
    @NotBlank String role) {}
