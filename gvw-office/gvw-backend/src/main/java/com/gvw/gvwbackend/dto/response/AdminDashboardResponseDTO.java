package com.gvw.gvwbackend.dto.response;

import java.util.List;
import java.util.Map;

public record AdminDashboardResponseDTO(
    int feedbackCount,
    int bugReportCount,
    double averageSentiment,
    String mostUsedHash,
    int userCount,
    long orphanedUserCount,
    List<Map<String, String>> deployments) {}
