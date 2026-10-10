package com.gvw.gvwbackend.model;

import java.util.Arrays;

/**
 * Represents the action type of a database migration (e.g., adding, deleting, renaming a field, or
 * running conditionally).
 */
public enum MigrationAction {
  ADD("add"),
  DELETE("delete"),
  RENAME("rename"),
  CONDITIONAL("conditional");

  private final String value;

  MigrationAction(String value) {
    this.value = value;
  }

  /**
   * Parses a string value case-insensitively into a corresponding {@link MigrationAction}. Throws
   * an {@link IllegalArgumentException} if the string is null or does not match any action.
   *
   * @param s The string value to parse.
   * @return The matching MigrationAction.
   * @throws IllegalArgumentException if the string is invalid or null.
   */
  public static MigrationAction fromString(String s) {
    if (s == null) throw new IllegalArgumentException("Bad Migration Action");
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Bad Migration Action"));
  }
}
