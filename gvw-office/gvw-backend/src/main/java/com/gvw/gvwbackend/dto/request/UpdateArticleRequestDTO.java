package com.gvw.gvwbackend.dto.request;

import com.gvw.gvwbackend.model.TextEditorBlock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateArticleRequestDTO(
    @NotBlank String id,
    @NotBlank String rev,
    @NotBlank String title,
    @NotNull List<@Valid TextEditorBlock> content) {}
