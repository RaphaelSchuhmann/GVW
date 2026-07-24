package com.gvw.gvwbackend.model;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum Role {
  ADMIN("admin"),
  SECRETARY("secretary"),
  BOARD_MEMBER("board_member"),
  LIBRARIAN("librarian"),
  CONDUCTOR("conductor"),
  MEMBER("member");

  private final String value;

  Role(String value) {
    this.value = value;
  }

  public static Role fromString(String s) {
    if (s == null) return MEMBER;
    return Arrays.stream(values())
        .filter(r -> r.value.equalsIgnoreCase(s))
        .findFirst()
        .orElse(MEMBER);
  }
}
