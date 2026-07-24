package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateEventStatusRequestDTO(@NotBlank String rev) {}
