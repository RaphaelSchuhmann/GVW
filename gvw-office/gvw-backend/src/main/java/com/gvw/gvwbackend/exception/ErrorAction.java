package com.gvw.gvwbackend.exception;

import lombok.Getter;

@Getter
public enum ErrorAction {
  READ_ONE(1),
  READ_ALL(2),
  CREATE(3),
  UPDATE(4),
  DELETE(5),
  CHECK(6),
  AUTH(7),
  UTILITY(99); // all helper endpoints or stuff that just doesn't really fit into the other options.

  private final int id;

  ErrorAction(int id) {
    this.id = id;
  }
}
