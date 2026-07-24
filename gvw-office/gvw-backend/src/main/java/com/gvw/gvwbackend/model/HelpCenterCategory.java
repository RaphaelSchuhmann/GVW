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
public class HelpCenterCategory {
  private String id;
  private String title;
  private String icon;
  private String description;
  private Boolean isFeatured;
  private Integer articleCount;

  @Builder.Default private final String type = "helpCenterCategory";
}
