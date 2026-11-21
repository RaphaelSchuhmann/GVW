package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gvw.gvwbackend.model.helper.StatusMessage;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

  @Builder.Default private List<Update> updates = new ArrayList<>();
  @Builder.Default private List<Migration> migrations = new ArrayList<>();

  public List<Migration> getMigrations() {
    if (this.migrations == null) {
      this.migrations = new ArrayList<>();
    }
    return this.migrations;
  }

  public void addUpdate(List<StatusMessage> messages) {
    for (StatusMessage msg : messages) {
      Update update =
          Update.builder().title(msg.title()).type(msg.type()).subTitle(msg.subTitle()).build();
      updates.add(update);
    }
  }

  public List<Update> getUpdates() {
    return updates.stream()
        .sorted(Comparator.comparing(Update::getTimestamp).reversed())
        .collect(Collectors.toList());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Update {
    @Builder.Default private String id = UUID.randomUUID().toString();
    @Builder.Default private OffsetDateTime timestamp = OffsetDateTime.now();

    private ErrorMessageType type;
    private String title;
    private String subTitle;
  }

  @Builder.Default private final String type = "deployment";
}
