package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Migration {
  private String id;
  private String database;
  private String field;
  private MigrationAction action;
  private Conditional conditional;
  private String value;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Conditional {
    private Branch condition;
    private Branch truePath;
    private Branch falsePath;
    private Boolean negated;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Branch {
    private String field;
    private String value;
  }
}
