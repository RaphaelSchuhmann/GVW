package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record UpdateMaxMembersRequestDTO(@Positive int maxMembers, @NotBlank String rev) {}
