package com.gvw.gvwbackend.model;

import java.util.Arrays;

/** Represents the action type of a deployment update */
public enum DeploymentUpdateType {
  SUCCESS("success"),
  INFO("info"),
  WARNING("warning"),
  ERROR("error");

  private final String value;

  DeploymentUpdateType(String value) {
    this.value = value;
  }

  /**
   * Parses a string value case-insensitively into a corresponding {@link DeploymentUpdateType}.
   * Throws an {@link IllegalArgumentException} if the string is null or does not match any action.
   *
   * @param s The string value to parse.
   * @return The matching update type.
   * @throws IllegalArgumentException if the string is invalid or null.
   */
  public static DeploymentUpdateType fromString(String s) {
    if (s == null) throw new IllegalArgumentException("Bad Deployment update type");
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Bad Deployment update type"));
  }
}
