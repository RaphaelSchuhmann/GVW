package com.gvw.gvwbackend.dto.response;

import com.gvw.gvwbackend.model.HelpCenterCategory;
import java.util.List;
import java.util.Map;

public record AppSettingsResponseDTO(
    int maxMembers,
    Map<String, String> scoreCategories,
    Map<String, String> feedbackCategories,
    String appVersion,
    List<HelpCenterCategory> helpCenterCategories,
    String rev) {
  public AppSettingsResponseDTO {
    scoreCategories = scoreCategories == null ? Map.of() : Map.copyOf(scoreCategories);
    feedbackCategories = feedbackCategories == null ? Map.of() : Map.copyOf(feedbackCategories);
    appVersion = appVersion == null ? "unknown" : appVersion;
    helpCenterCategories =
        helpCenterCategories == null ? List.of() : List.copyOf(helpCenterCategories);
  }
}
