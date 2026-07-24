package com.gvw.gvwbackend.exception;

import lombok.Getter;

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
