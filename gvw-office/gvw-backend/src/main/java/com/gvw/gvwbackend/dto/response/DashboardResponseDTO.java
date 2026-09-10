package com.gvw.gvwbackend.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardResponseDTO(
    List<DashboardMemberSummaryDTO> members,
    int totalEvents,
    List<DashboardEventSummaryDTO> upcomingEvents,
    List<Map<String, String>> upcomingBirthdays,
    int totalScores) {}
