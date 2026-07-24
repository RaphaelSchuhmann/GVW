package com.gvw.gvwbackend.dto.response;

import com.gvw.gvwbackend.model.TextEditorBlock;
import java.util.List;

public record FullArticleResponseDTO(
    String id,
    String rev,
    String title,
    String description,
    List<TextEditorBlock> content,
    int readingTimeInMinutes) {}
