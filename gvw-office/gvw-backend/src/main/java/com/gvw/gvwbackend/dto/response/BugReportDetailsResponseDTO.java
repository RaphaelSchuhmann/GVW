package com.gvw.gvwbackend.dto.response;

import java.time.LocalDateTime;

public record BugReportDetailsResponseDTO(
    String title,
    String severity,
    String stepsToReproduce,
    String userEmail,
    LocalDateTime timestamp,
    String appVersion,
    String route,
    String os,
    String browser,
    String viewport) {}
