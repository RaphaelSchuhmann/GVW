package com.gvw.gvwbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateDocumentAttachmentsDTO(
    @NotBlank String rev, @NotNull List<String> attachments) {}
