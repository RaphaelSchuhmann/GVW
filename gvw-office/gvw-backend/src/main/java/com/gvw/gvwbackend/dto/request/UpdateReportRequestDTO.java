package com.gvw.gvwbackend.dto.request;

import com.gvw.gvwbackend.model.TextEditorBlock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateReportRequestDTO(
    @NotBlank String id,
    @NotBlank String rev,
    @NotBlank String title,
    @NotBlank String editor,
    @NotNull List<@Valid TextEditorBlock> content) {}
