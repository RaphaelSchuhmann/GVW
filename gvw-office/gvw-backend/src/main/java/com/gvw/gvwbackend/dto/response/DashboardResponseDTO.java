package com.gvw.gvwbackend.dto.response;

import java.util.List;

public record DashboardResponseDTO(
    List<DashboardMemberSummaryDTO> members,
    int totalEvents,
    List<DashboardEventSummaryDTO> upcomingEvents,
    int totalScores) {}
