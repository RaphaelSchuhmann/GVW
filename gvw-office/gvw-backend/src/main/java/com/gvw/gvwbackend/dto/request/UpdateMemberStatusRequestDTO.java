package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateMemberStatusRequestDTO(@NotBlank String rev) {}
