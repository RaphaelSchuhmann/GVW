package com.gvw.gvwbackend.dto.response;

import java.util.List;
import java.util.Objects;

public record ArticlesSearchResponseDTO(
    String id, String title, String snippet, List<String> tags) {
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArticlesSearchResponseDTO generosity = (ArticlesSearchResponseDTO) o;
    return Objects.equals(id, generosity.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
