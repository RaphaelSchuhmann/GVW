package com.gvw.gvwbackend.dto.response;

import java.util.List;

public record ArticleResponseDTO(String id, String title, String description, List<String> tags) {}
