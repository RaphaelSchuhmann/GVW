package com.gvw.gvwbackend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextDocumentSearchResult<T> {
  private T document;
  private String snippet;
}
