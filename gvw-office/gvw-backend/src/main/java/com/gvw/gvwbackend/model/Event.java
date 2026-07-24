package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String title;
  private String type;
  private String date;
  private LocalTime time;
  private String location;
  private String description;
  private String mode;
  private String status;
  private Recurrence recurrence;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Recurrence {
    private String monthlyKind;
    private String dayOfMonth;
    private String weekDay;
    private String ordinal;
  }

  @Builder.Default
  @JsonProperty("doc_type")
  private final String docType = "event";
}
