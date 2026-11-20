package com.gvw.gvwbackend.model.deploymentSystem;

import java.util.Arrays;

public enum HealthStatus {
  HEALTHY("healthy"),
  UNHEALTHY("unhealthy");

  private final String value;

  HealthStatus(String value) {
    this.value = value;
  }

  /**
   * Parses a string value case-insensitively into a corresponding {@link HealthStatus}. Returns
   * {@link #UNHEALTHY} as a default fallback if the string is null or unrecognized.
   *
   * @param s The string value to parse.
   * @return The matching HealthStatus, or UNHEALTHY by default.
   */
  public static HealthStatus fromString(String s) {
    if (s == null) return UNHEALTHY;
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElse(UNHEALTHY);
  }
}
