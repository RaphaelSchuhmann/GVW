package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.*;
import com.gvw.gvwbackend.dto.response.AppSettingsResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.AppSettings;
import com.gvw.gvwbackend.model.HelpCenterCategory;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AppSettingsService {
  private static final Logger log = LoggerFactory.getLogger(AppSettingsService.class);
  private final DbService dbService;
  private final SseService sseService;
  private static final Set<String> BLOCKED_KEYS = Set.of("__proto__", "constructor", "prototype");

  public AppSettingsService(DbService dbService, SseService sseService) {
    this.dbService = dbService;
    this.sseService = sseService;
  }

  public AppSettingsResponseDTO getAppSettings() {
    AppSettings settings = appSettings(ErrorAction.READ_ONE, ErrorResource.NONE);

    return new AppSettingsResponseDTO(
        settings.getMaxMembers(),
        settings.getScoreCategories(),
        settings.getFeedbackCategories(),
        settings.getAppVersion(),
        settings.getHelpCenterCategories(),
        settings.getRev());
  }

  public String updateMaxMembers(UpdateMaxMembersRequestDTO requestDTO) {
    AppSettings settings = appSettings(ErrorAction.UPDATE, ErrorResource.NONE);
    settings.setMaxMembers(requestDTO.maxMembers());
    settings.setRev(requestDTO.rev());

    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    Object revObj = resp != null ? resp.get("rev") : null;
    if (!(revObj instanceof String) || ((String) revObj).isBlank()) {
      throw new RuntimeException(
          String.valueOf(ErrorDomain.APP_SETTINGS.createCode(ErrorAction.UPDATE, 500)));
    }

    String rev = (String) revObj;
    try {
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast SETTINGS refresh: ", ex);
    }
    return rev;
  }

  public String addCategory(AddCategoryRequestDTO requestDTO) {
    if (requestDTO.type() == null
        || requestDTO.displayName() == null
        || BLOCKED_KEYS.contains(requestDTO.type())
        || BLOCKED_KEYS.contains(requestDTO.displayName())) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.UPDATE, 400, ErrorResource.LIBRARY_CATEGORY)));
    }

    AppSettings settings = appSettings(ErrorAction.UPDATE, ErrorResource.LIBRARY_CATEGORY);
    Map<String, String> categories = settings.getScoreCategories();

    if (categories.containsKey(requestDTO.type())
        || categories.containsKey(requestDTO.displayName())) {
      throw new ConflictException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.UPDATE, 409, ErrorResource.LIBRARY_CATEGORY)));
    }

    categories.put(requestDTO.type(), requestDTO.displayName());
    categories.put(requestDTO.displayName(), requestDTO.type());

    settings.setScoreCategories(categories);

    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    try {
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast SETTINGS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.APP_SETTINGS.createCode(ErrorAction.UPDATE, 500)));
  }

  public String removeCategory(RemoveCategoryRequestDTO requestDTO) {
    String type = requestDTO.type();
    if (type == null) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.DELETE, 400, ErrorResource.LIBRARY_CATEGORY)));
    }

    AppSettings settings = appSettings(ErrorAction.DELETE, ErrorResource.LIBRARY_CATEGORY);
    Map<String, String> categories = settings.getScoreCategories();

    String displayName = categories.get(type);

    categories.remove(type);
    if (displayName != null) categories.remove(displayName);

    settings.setScoreCategories(categories);
    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    try {
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast SETTINGS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.APP_SETTINGS.createCode(ErrorAction.DELETE, 500)));
  }

  public String addHelpCenterCategoryToSettings(AddHelpCenterCategoryRequestDTO dto) {
    AppSettings settings = appSettings(ErrorAction.CREATE, ErrorResource.HELP_CENTER_CATEGORY);

    List<HelpCenterCategory> categories =
        settings.getHelpCenterCategories() != null
            ? new ArrayList<>(settings.getHelpCenterCategories())
            : new ArrayList<>();

    if (categories.stream().anyMatch(obj -> dto.title().equals(obj.getTitle()))) {
      throw new ConflictException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.CREATE, 409, ErrorResource.HELP_CENTER_CATEGORY)));
    }

    categories.add(
        HelpCenterCategory.builder()
            .id(UUID.randomUUID().toString())
            .title(dto.title())
            .icon(dto.icon())
            .description(dto.description())
            .articleCount(0)
            .isFeatured(false)
            .build());

    settings.setHelpCenterCategories(categories);

    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    try {
      sseService.broadcastRefresh("HELP_CENTER");
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast HELP_CENTER or SETTINGS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(
            ErrorDomain.APP_SETTINGS.createCode(
                ErrorAction.CREATE, 500, ErrorResource.HELP_CENTER_CATEGORY)));
  }

  public String removeHelpCenterCategoryFromSettings(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.DELETE, 400, ErrorResource.HELP_CENTER_CATEGORY)));
    }

    AppSettings settings = appSettings(ErrorAction.DELETE, ErrorResource.HELP_CENTER_CATEGORY);
    List<HelpCenterCategory> categories = settings.getHelpCenterCategories();

    if (categories == null || categories.stream().noneMatch(obj -> obj.getId().equals(id)))
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.DELETE, 404, ErrorResource.HELP_CENTER_CATEGORY)));

    categories.removeIf(obj -> obj.getId().equals(id));
    settings.setHelpCenterCategories(categories);

    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    try {
      sseService.broadcastRefresh("HELP_CENTER");
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast HELP_CENTER or SETTINGS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.APP_SETTINGS.createCode(ErrorAction.DELETE, 500)));
  }

  public String updateFeaturedHelpCenterCategories(
      SetFeaturedHelpCenterCategoriesRequestDTO request) {
    AppSettings settings = appSettings(ErrorAction.UPDATE, ErrorResource.HELP_CENTER_CATEGORY);
    List<HelpCenterCategory> categories = settings.getHelpCenterCategories();

    if (categories != null) {
      categories.forEach(
          obj -> {
            if (request.featured().containsKey(obj.getId())) {
              obj.setIsFeatured(request.featured().get(obj.getId()));
            }
          });
    }
    settings.setHelpCenterCategories(categories);

    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    try {
      sseService.broadcastRefresh("HELP_CENTER");
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast HELP_CENTER or SETTINGS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.APP_SETTINGS.createCode(ErrorAction.UPDATE, 500)));
  }

  public String updateHelpCenterCategoryArticleCount(String id, int newCount) {
    if (id == null || id.isBlank() || newCount < 0) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.UPDATE, 400, ErrorResource.HELP_CENTER_CATEGORY)));
    }

    AppSettings settings = appSettings(ErrorAction.UPDATE, ErrorResource.HELP_CENTER_CATEGORY);
    List<HelpCenterCategory> categories = settings.getHelpCenterCategories();

    if (categories == null) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.APP_SETTINGS.createCode(
                  ErrorAction.UPDATE, 404, ErrorResource.HELP_CENTER_CATEGORY)));
    }

    HelpCenterCategory category =
        categories.stream()
            .filter(cat -> id.equals(cat.getId()))
            .findFirst()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.valueOf(
                            ErrorDomain.APP_SETTINGS.createCode(
                                ErrorAction.UPDATE, 404, ErrorResource.HELP_CENTER_CATEGORY))));

    category.setArticleCount(newCount);

    Map<String, Object> resp = dbService.update("app_settings", settings.getId(), settings);

    try {
      sseService.broadcastRefresh("SETTINGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast HELP_CENTER or SETTINGS refresh: ", ex);
    }

    if (resp != null && resp.get("rev") instanceof String rev) {
      return rev;
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.APP_SETTINGS.createCode(ErrorAction.UPDATE, 500)));
  }

  public AppSettings appSettings(ErrorAction action, ErrorResource resource) {
    AppSettings settings = dbService.findById("app_settings", "general", AppSettings.class);

    if (settings == null) {
      log.error("App settings not found");
      throw new NotFoundException(
          String.valueOf(ErrorDomain.APP_SETTINGS.createCode(action, 404, resource)));
    }

    return settings;
  }
}
