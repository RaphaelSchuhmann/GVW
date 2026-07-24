package com.gvw.gvwbackend.dto.response;

public record ReportSearchResponseDTO(
    String id, String title, String author, String type, String snippet, String createdAt) {}
