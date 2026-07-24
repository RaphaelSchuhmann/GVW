package com.gvw.gvwbackend.dto.response;

import java.util.List;

public record ScoreResponseDTO(
    String id,
    String rev,
    String scoreId,
    String title,
    String artist,
    String type,
    List<String> voices,
    int voiceCount,
    List<String> files) {}
