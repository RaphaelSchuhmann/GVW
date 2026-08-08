package com.gvw.gvwbackend.exception;

import lombok.Getter;

/**
 * Defines optional sub-resources affected by an error.
 *
 * <p>Resources are appended to the error code when a domain contains multiple independently
 * addressable entities.
 *
 * <p>{@link #NONE} indicates that no specific resource applies.
 */
@Getter
public enum ErrorResource {
  NONE(0),
  HELP_CENTER_CATEGORY(1),
  HELP_CENTER_ARTICLE(2),
  LIBRARY_CATEGORY(3),
  TEXT_EDITOR_CONTENT(4);

  private final int id;

  ErrorResource(int id) {
    this.id = id;
  }
}
