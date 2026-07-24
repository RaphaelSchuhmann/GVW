package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SetFeaturedHelpCenterCategoriesRequestDTO(@NotNull Map<String, Boolean> featured) {}
