package com.gvw.gvwbackend.exception;

import lombok.Getter;

/**
 * Defines the operation that caused an application error.
 *
 * <p>The action is encoded into the second section of the GVW error code.
 */
@Getter
public enum ErrorAction {
  READ_ONE(1),
  READ_ALL(2),
  CREATE(3),
  UPDATE(4),
  DELETE(5),
  CHECK(6),
  AUTH(7),
  UTILITY(99); // Used for helper endpoints or operations without a clear CRUD category

  private final int id;

  ErrorAction(int id) {
    this.id = id;
  }
}
