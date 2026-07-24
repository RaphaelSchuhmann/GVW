package com.gvw.gvwbackend.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportMetaData {
  private String userId;
  private String route;
  private String appVersion;
  private LocalDateTime timestamp;
  private String os;
  private String browser;
  private String viewport;

  @Builder.Default private final String type = "reportMetaData";
}
