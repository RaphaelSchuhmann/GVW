package com.gvw.gvwbackend.model.helper;

import java.util.Arrays;

public enum ErrorMessageType {
  SUCCESS("success"),
  INFO("info"),
  WARNING("warning"),
  ERROR("error");

  private final String value;

  ErrorMessageType(String value) {
    this.value = value;
  }

  /**
   * Parses a string value case-insensitively into a corresponding {@link ErrorMessageType}. Returns
   * {@link #INFO} as a default fallback if the string is null or unrecognized.
   *
   * @param s The string value to parse.
   * @return The matching ErrorMessageType, or INFO by default.
   */
  public static ErrorMessageType fromString(String s) {
    if (s == null) return INFO;
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElse(INFO);
  }
}
