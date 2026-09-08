package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.*;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.AppSettings;
import com.gvw.gvwbackend.model.HelpCenterCategory;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppSettingsServiceTest {

  @Mock private DbService dbService;

  @Mock private SseService sseService;

  @InjectMocks private AppSettingsService appSettingsService;

  private AppSettings appSettings;

  @BeforeEach
  void setUp() {
    appSettings = new AppSettings();
    appSettings.setId("general");
    appSettings.setRev("1-abc");
    appSettings.setMaxMembers(10);
    appSettings.setScoreCategories(new HashMap<>());
    appSettings.setHelpCenterCategories(new ArrayList<>());
  }

  @Test
  void updateMaxMembers_Success() {
    UpdateMaxMembersRequestDTO request = new UpdateMaxMembersRequestDTO(20, "1-abc");
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.updateMaxMembers(request);

    assertEquals(20, appSettings.getMaxMembers());
    assertEquals("2-def", result);
    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void addCategory_Success() {
    AddCategoryRequestDTO request = new AddCategoryRequestDTO("test-type", "Test Category");

    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.addCategory(request);

    assertEquals("Test Category", appSettings.getScoreCategories().get("test-type"));
    assertEquals("test-type", appSettings.getScoreCategories().get("Test Category"));
    assertEquals("2-def", result);

    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void addCategory_BlockedKey_ThrowsBadRequest() {
    AddCategoryRequestDTO request = new AddCategoryRequestDTO("__proto__", "Test");
    assertThrows(BadRequestException.class, () -> appSettingsService.addCategory(request));
  }

  @Test
  void addCategory_DuplicateType_ThrowsConflict() {
    appSettings.getScoreCategories().put("test-type", "Test Category");
    AddCategoryRequestDTO request = new AddCategoryRequestDTO("test-type", "New Category");
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);

    assertThrows(ConflictException.class, () -> appSettingsService.addCategory(request));
  }

  @Test
  void removeCategory_Success() {
    appSettings.getScoreCategories().put("test-type", "Test Category");
    appSettings.getScoreCategories().put("Test Category", "test-type");
    RemoveCategoryRequestDTO request = new RemoveCategoryRequestDTO("test-type");
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.removeCategory(request);

    assertFalse(appSettings.getScoreCategories().containsKey("test-type"));
    assertFalse(appSettings.getScoreCategories().containsKey("Test Category"));
    assertEquals("2-def", result);
    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void removeCategory_NullType_ThrowsBadRequest() {
    RemoveCategoryRequestDTO request = new RemoveCategoryRequestDTO(null);
    assertThrows(BadRequestException.class, () -> appSettingsService.removeCategory(request));
  }

  @Test
  void addHelpCenterCategoryToSettings_Success() {
    AddHelpCenterCategoryRequestDTO dto =
        new AddHelpCenterCategoryRequestDTO("Test Category", "icon", "description");
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.addHelpCenterCategoryToSettings(dto);

    assertEquals(1, appSettings.getHelpCenterCategories().size());
    assertEquals("Test Category", appSettings.getHelpCenterCategories().get(0).getTitle());
    assertEquals("2-def", result);
    verify(sseService).sendRefresh("HELP_CENTER");
    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void addHelpCenterCategoryToSettings_DuplicateTitle_ThrowsConflict() {
    HelpCenterCategory existingCategory =
        HelpCenterCategory.builder()
            .id("1")
            .title("Test Category")
            .icon("icon")
            .description("desc")
            .articleCount(0)
            .isFeatured(false)
            .build();
    appSettings.setHelpCenterCategories(List.of(existingCategory));
    AddHelpCenterCategoryRequestDTO dto =
        new AddHelpCenterCategoryRequestDTO("Test Category", "icon", "description");
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);

    assertThrows(
        ConflictException.class, () -> appSettingsService.addHelpCenterCategoryToSettings(dto));
  }

  @Test
  void removeHelpCenterCategoryFromSettings_Success() {
    HelpCenterCategory category =
        HelpCenterCategory.builder()
            .id("cat-1")
            .title("Test")
            .icon("icon")
            .description("desc")
            .articleCount(0)
            .isFeatured(false)
            .build();

    appSettings.setHelpCenterCategories(new ArrayList<>(List.of(category)));

    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.removeHelpCenterCategoryFromSettings("cat-1");

    assertTrue(appSettings.getHelpCenterCategories().isEmpty());
    assertEquals("2-def", result);
    verify(sseService).sendRefresh("HELP_CENTER");
    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void removeHelpCenterCategoryFromSettings_NullId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> appSettingsService.removeHelpCenterCategoryFromSettings(null));
  }

  @Test
  void removeHelpCenterCategoryFromSettings_NotFound_ThrowsNotFound() {
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);

    assertThrows(
        NotFoundException.class,
        () -> appSettingsService.removeHelpCenterCategoryFromSettings("non-existent"));
  }

  @Test
  void updateFeaturedHelpCenterCategories_Success() {
    HelpCenterCategory category =
        HelpCenterCategory.builder()
            .id("cat-1")
            .title("Test")
            .icon("icon")
            .description("desc")
            .articleCount(0)
            .isFeatured(false)
            .build();
    appSettings.setHelpCenterCategories(List.of(category));
    SetFeaturedHelpCenterCategoriesRequestDTO request =
        new SetFeaturedHelpCenterCategoriesRequestDTO(Map.of("cat-1", true));
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.updateFeaturedHelpCenterCategories(request);

    assertTrue(appSettings.getHelpCenterCategories().get(0).getIsFeatured());
    assertEquals("2-def", result);
    verify(sseService).sendRefresh("HELP_CENTER");
    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void updateHelpCenterCategoryArticleCount_Success() {
    HelpCenterCategory category =
        HelpCenterCategory.builder()
            .id("cat-1")
            .title("Test")
            .icon("icon")
            .description("desc")
            .articleCount(5)
            .isFeatured(false)
            .build();
    appSettings.setHelpCenterCategories(List.of(category));
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);
    when(dbService.update("app_settings", "general", appSettings)).thenReturn("2-def");

    String result = appSettingsService.updateHelpCenterCategoryArticleCount("cat-1", 10);

    assertEquals(10, appSettings.getHelpCenterCategories().get(0).getArticleCount());
    assertEquals("2-def", result);
    verify(sseService).sendRefresh("SETTINGS");
  }

  @Test
  void updateHelpCenterCategoryArticleCount_NullId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> appSettingsService.updateHelpCenterCategoryArticleCount(null, 10));
  }

  @Test
  void updateHelpCenterCategoryArticleCount_NegativeCount_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> appSettingsService.updateHelpCenterCategoryArticleCount("cat-1", -1));
  }

  @Test
  void updateHelpCenterCategoryArticleCount_CategoryNotFound_ThrowsNotFound() {
    when(dbService.findById("app_settings", "general", AppSettings.class)).thenReturn(appSettings);

    assertThrows(
        NotFoundException.class,
        () -> appSettingsService.updateHelpCenterCategoryArticleCount("non-existent", 10));
  }
}
