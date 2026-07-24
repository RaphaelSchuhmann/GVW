package com.gvw.gvwbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class File {
  private String id;
  private String originalName;
  private String mimeType;
  private long size;
  private String extension;

  @Builder.Default private final String type = "file";
}
