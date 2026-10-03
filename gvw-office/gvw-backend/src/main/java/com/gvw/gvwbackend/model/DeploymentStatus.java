package com.gvw.gvwbackend.model;

import java.util.Arrays;

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

  public static DeploymentStatus fromString(String s) {
    if (s == null) return SCHEDULED;
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElse(SCHEDULED);
  }
}
