package com.gvw.gvwbackend.dto.response;

import com.gvw.gvwbackend.model.TextEditorBlock;
import java.util.List;

public record FullReportResponseDTO(
    String id,
    String title,
    String author,
    String rev,
    String description,
    int readingTimeInMinutes,
    int wordCount,
    String createdAt,
    String lastEditedBy,
    String type,
    List<TextEditorBlock> content,
    List<String> attachments) {}
