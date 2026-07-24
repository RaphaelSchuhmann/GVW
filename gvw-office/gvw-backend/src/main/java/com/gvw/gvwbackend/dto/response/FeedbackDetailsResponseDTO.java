package com.gvw.gvwbackend.dto.response;

import java.time.LocalDateTime;

public record FeedbackDetailsResponseDTO(
    String title,
    String category,
    String message,
    Integer sentiment,
    String userEmail,
    LocalDateTime timestamp,
    String appVersion,
    String route) {}
