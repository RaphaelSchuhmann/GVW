package com.gvw.gvwbackend.dto.response;

import java.time.Instant;

public record ChangelogResponseDTO(
    String id, String title, String version, String content, Instant timestamp) {}
