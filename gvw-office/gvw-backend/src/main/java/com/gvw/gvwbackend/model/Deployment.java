package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Deployment {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String title;
  private String appVersion;
  private String date;
  private LocalTime startTime;
  private LocalTime endTime;
  private String commit;
  private String hash;
  private DeploymentStatus status;
  private Integer error; // Optional error if the deployment gets canceled or fails

  @Builder.Default private List<Migration> migrations = new ArrayList<>();

  public List<Migration> getMigrations() {
    if (this.migrations == null) {
      this.migrations = new ArrayList<>();
    }
    return this.migrations;
  }

  @Builder.Default private final String type = "deployment";
}
