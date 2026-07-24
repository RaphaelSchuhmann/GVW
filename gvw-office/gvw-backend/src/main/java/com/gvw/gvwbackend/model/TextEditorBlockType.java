package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TextEditorBlockType {
  TEXT("text"),
  IMAGE("image"),
  BLOCKQUOTE("blockquote"),
  HEADING_T1("h1"),
  HEADING_T2("h2"),
  HEADING_T3("h3"),
  HEADING_T4("h4");

  private final String value;

  TextEditorBlockType(String value) {
    this.value = value;
  }

  @JsonCreator
  public static TextEditorBlockType fromString(String s) {
    if (s == null) return TEXT;
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElse(TEXT);
  }

  @JsonValue
  public String toJson() {
    return value;
  }
}
