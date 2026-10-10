package com.gvw.gvwbackend.model;

import java.util.Arrays;

/** Represents the lifecycle status of a deployment. */
public enum DeploymentStatus {
  SCHEDULED("scheduled"),
  RUNNING("running"),
  SUCCESSFUL("successful"),
  CANCELLED("cancelled"),
  FAILED("failed");

  private final String value;

  DeploymentStatus(String value) {
    this.value = value;
  }

  /**
   * Parses a string value case-insensitively into a corresponding {@link DeploymentStatus}. Returns
   * {@link #SCHEDULED} as a default fallback if the string is null or unrecognized.
   *
   * @param s The string value to parse.
   * @return The matching DeploymentStatus, or SCHEDULED by default.
   */
  public static DeploymentStatus fromString(String s) {
    if (s == null) return SCHEDULED;
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElse(SCHEDULED);
  }
}
