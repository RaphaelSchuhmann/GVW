package com.gvw.gvwbackend.model;

import java.util.Arrays;

public enum MigrationAction {
  ADD("add"),
  DELETE("delete"),
  RENAME("rename"),
  CONDITIONAL("conditional");

  private final String value;

  MigrationAction(String value) {
    this.value = value;
  }

  public static MigrationAction fromString(String s) {
    if (s == null) throw new IllegalArgumentException("Bad Migration Action");
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Bad Migration Action"));
  }
}
