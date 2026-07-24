package com.gvw.gvwbackend.dto.response;

public record ReportResponseDTO(
    String id, String title, String author, String type, String description, String createdAt) {}
