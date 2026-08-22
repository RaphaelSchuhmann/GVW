package com.gvw.gvwbackend.dto.response;

public record AdminDashboardResponseDTO(
    int feedbackCount,
    int bugReportCount,
    double averageSentiment,
    String mostUsedHash,
    int userCount,
    long orphanedUserCount) {}
