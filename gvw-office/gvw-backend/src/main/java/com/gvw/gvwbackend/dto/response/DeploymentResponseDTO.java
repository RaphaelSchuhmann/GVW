package com.gvw.gvwbackend.dto.response;

import com.gvw.gvwbackend.model.DeploymentStatus;
import com.gvw.gvwbackend.model.Migration;
import java.time.LocalTime;
import java.util.List;

public record DeploymentResponseDTO(
    String id,
    String rev,
    String title,
    DeploymentStatus status,
    String date,
    LocalTime startTime,
    LocalTime endTime,
    String commit,
    String hash,
    List<Migration> migrations,
    String appVersion) {}
