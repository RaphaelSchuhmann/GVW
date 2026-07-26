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

/**
 * Service responsible for managing application-wide settings.
 *
 * <p>The application settings are stored as a single CouchDB document with the
 * fixed identifier {@code general}. This service provides controlled access to
 * modifying those settings and handles synchronization notifications through SSE.
 *
 * <p>Settings managed by this service include:
 * <ul>
 *   <li>Member limits</li>
 *   <li>Library score categories</li>
 *   <li>Help center categories</li>
 *   <li>Application metadata</li>
 * </ul>
 *
 * <p>Changes made through this service should trigger an SSE refresh event so
 * connected clients can update their local state.
 */
@Service
public class AppSettingsService {
  private static final Logger log = LoggerFactory.getLogger(AppSettingsService.class);
  private final DbService dbService;
  private final SseService sseService;

  /**
   * Keys that are rejected for user-defined configuration values.
   *
   * <p>These values are commonly involved in prototype pollution attacks in
   * JavaScript environments. Although the backend is written in Java, these
   * values are eventually consumed by the frontend and therefore must not be
   * accepted as dynamic configuration keys.
   */
  private static final Set<String> BLOCKED_KEYS = Set.of("__proto__", "constructor", "prototype");

  public AppSettingsService(DbService dbService, SseService sseService) {
    this.dbService = dbService;
    this.sseService = sseService;
  }

  /**
   * Retrieves the current application settings.
   *
   * @return current application configuration including the CouchDB revision
   * @throws NotFoundException if the settings document does not exist
   */
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

  /**
   * Updates the maximum allowed number of members.
   *
   * <p>The CouchDB revision supplied by the client is used to prevent accidental
   * overwrites of newer changes.
   *
   * @param requestDTO update request containing the new limit and document revision
   * @return updated CouchDB revision
   */
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

  /**
   * Adds a new library score category.
   *
   * <p>Categories are stored as a bidirectional mapping:
   * <pre>
   * type -> display name
   * display name -> type
   * </pre>
   *
   * <p>This allows conversion between internal category identifiers and their
   * user-facing names without additional database queries.
   *
   * @param requestDTO category data
   * @return updated CouchDB revision
   */
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

  /**
   * Removes a library score category and its reverse lookup entry.
   *
   * <p>Because categories are stored bidirectionally, both the internal type
   * and display name entries must be removed.
   */
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

  /**
   * Adds a new help center category to the application settings document.
   *
   * <p>Help center categories are embedded directly inside the settings document
   * instead of being stored as separate database documents.
   *
   * <p>A generated UUID is used as the category identifier because the category
   * needs a stable reference for article counters and featured state updates.
   */
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

  /**
   * Removes a help center category from the global application settings.
   *
   * <p>Help center categories are currently stored as embedded objects inside the
   * application settings document. Removing a category updates the settings
   * document and notifies connected clients to refresh both help center data and
   * application settings.
   *
   * @param id unique identifier of the help center category to remove
   * @return the new CouchDB document revision after the update
   * @throws BadRequestException if the category ID is missing or invalid
   * @throws NotFoundException if no category with the given ID exists
   * @throws RuntimeException if the database update fails
   */
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

  /**
   * Updates the featured state of help center categories.
   *
   * <p>The request contains a map of category IDs to their desired featured state.
   * Only categories included in the request are modified.
   *
   * @param request requested featured states
   * @return updated CouchDB revision
   */
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

  /**
   * Updates the cached article count of a help center category.
   *
   * <p>The article count is stored in settings instead of calculated dynamically
   * to avoid additional database queries when loading the help center overview.
   *
   * @param id category identifier
   * @param newCount new article count
   * @return updated CouchDB revision
   */
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

  /**
   * Loads the global application settings document.
   *
   * <p>All settings operations use this method to ensure consistent handling
   * of missing configuration data and error reporting.
   *
   * @param action action being performed for error reporting
   * @param resource affected resource for error reporting
   * @return application settings document
   */
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
