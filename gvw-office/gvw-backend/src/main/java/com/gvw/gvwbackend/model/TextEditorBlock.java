package com.gvw.gvwbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextEditorBlock {
  private String id;
  private TextEditorBlockType type;
  private String data;
}
