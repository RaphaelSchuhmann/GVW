package com.gvw.gvwbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gvw.gvwbackend.model.Event;
import java.time.LocalTime;

public record EventResponseDTO(
    String id,
    String rev,
    String title,
    String type,
    String date,
    @JsonFormat(pattern = "HH:mm") LocalTime time,
    String location,
    String description,
    String mode,
    String status,
    Event.Recurrence recurrence) {}
