package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BugReport {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String title;
  private String severity;
  private String stepsToReproduce;
  private UserReportMetaData metaData;

  @Builder.Default
  @JsonProperty("doc_type")
  private final String docType = "bugReport";
}
