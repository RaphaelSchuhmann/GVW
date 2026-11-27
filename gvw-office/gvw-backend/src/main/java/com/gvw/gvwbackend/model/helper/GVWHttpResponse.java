package com.gvw.gvwbackend.model.helper;

public record GVWHttpResponse(int status, Object body, String contentType, boolean isError) {
  public GVWHttpResponse(int status, Object body, String contentType) {
    this(status, body, contentType, false);
  }
}
