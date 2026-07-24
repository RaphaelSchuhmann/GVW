package com.gvw.gvwbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gvw.gvwbackend.model.Event;
import java.time.LocalTime;

public record DashboardEventSummaryDTO(
    String title,
    String date,
    @JsonFormat(pattern = "HH:mm") LocalTime time,
    String location,
    String type,
    String mode,
    Event.Recurrence recurrence) {}
