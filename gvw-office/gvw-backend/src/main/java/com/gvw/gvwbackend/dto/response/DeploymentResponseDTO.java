package com.gvw.gvwbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gvw.gvwbackend.model.DeploymentStatus;
import com.gvw.gvwbackend.model.Migration;
import java.time.LocalTime;
import java.util.List;

public record DeploymentResponseDTO(
    String id,
    String rev,
    String title,
    DeploymentStatus type,
    String date,
    @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm") LocalTime endTime,
    String commit,
    String hash,
    List<Migration> migrations,
    String appVersion) {}
