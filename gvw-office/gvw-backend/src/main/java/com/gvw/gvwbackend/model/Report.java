package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class Report implements TextDocument {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String title;
  private String author;
  private String description;
  private String type;
  private String createdAt;
  private String lastEditedBy;

  private List<TextEditorBlock> contents;
  private List<File> attachments;

  @Builder.Default
  @JsonProperty("doc_type")
  private final String docType = "report";
}
