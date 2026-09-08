package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddHelpCenterArticleRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateArticleRequestDTO;
import com.gvw.gvwbackend.dto.response.ArticleResponseDTO;
import com.gvw.gvwbackend.dto.response.FullArticleResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.AppSettings;
import com.gvw.gvwbackend.model.HelpCenterArticle;
import com.gvw.gvwbackend.model.HelpCenterCategory;
import com.gvw.gvwbackend.model.TextEditorBlock;
import com.gvw.gvwbackend.model.TextEditorBlockType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HelpCenterServiceTest {

  @Mock private DbService dbService;

  @Mock private SseService sseService;

  @Mock private AppSettingsService appSettingsService;

  @Mock private TextEditorService editorService;

  @InjectMocks private HelpCenterService helpCenterService;

  private AppSettings appSettings;
  private HelpCenterCategory category;
  private HelpCenterArticle article;

  @BeforeEach
  void setUp() {
    category =
        HelpCenterCategory.builder()
            .id("cat-1")
            .title("Test Category")
            .icon("icon")
            .description("desc")
            .articleCount(0)
            .isFeatured(false)
            .build();

    appSettings = new AppSettings();
    appSettings.setHelpCenterCategories(new ArrayList<>(List.of(category)));

    article =
        HelpCenterArticle.builder()
            .id("article-1")
            .rev("1-abc")
            .title("Test Article")
            .description("Test description")
            .category("cat-1")
            .tags(List.of("tag1"))
            .contents(List.of(new TextEditorBlock()))
            .build();
  }

  @Test
  void removeHelpCenterCategory_Success() {
    when(dbService.findByQuery(
            "help_center",
            Map.of("selector", Map.of("category", "cat-1")),
            HelpCenterArticle.class))
        .thenReturn(List.of());
    when(appSettingsService.removeHelpCenterCategoryFromSettings("cat-1")).thenReturn("2-def");

    String result = helpCenterService.removeHelpCenterCategory("cat-1");

    assertEquals("2-def", result);
  }

  @Test
  void removeHelpCenterCategory_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> helpCenterService.removeHelpCenterCategory(null));
  }

  @Test
  void removeHelpCenterCategory_HasArticles_ThrowsConflict() {
    when(dbService.findByQuery(
            "help_center",
            Map.of("selector", Map.of("category", "cat-1")),
            HelpCenterArticle.class))
        .thenReturn(List.of(article));

    assertThrows(
        ConflictException.class, () -> helpCenterService.removeHelpCenterCategory("cat-1"));
  }

  @Test
  void createArticle_Success() {
    AddHelpCenterArticleRequestDTO dto =
        new AddHelpCenterArticleRequestDTO("New Article", "Description", List.of("tag"), "cat-1");
    when(appSettingsService.appSettings(ErrorAction.CREATE, ErrorResource.HELP_CENTER_ARTICLE))
        .thenReturn(appSettings);
    when(appSettingsService.updateHelpCenterCategoryArticleCount("cat-1", 1)).thenReturn("2-def");

    String result = helpCenterService.createArticle(dto);

    assertEquals("2-def", result);
    verify(dbService).insert(eq("help_center"), any(HelpCenterArticle.class));
    verify(sseService).sendRefresh("HELP_CENTER");
  }

  @Test
  void createArticle_InvalidCategory_ThrowsBadRequest() {
    AddHelpCenterArticleRequestDTO dto =
        new AddHelpCenterArticleRequestDTO(
            "New Article", "Description", List.of("tag"), "invalid-cat");
    when(appSettingsService.appSettings(ErrorAction.CREATE, ErrorResource.HELP_CENTER_ARTICLE))
        .thenReturn(appSettings);

    assertThrows(BadRequestException.class, () -> helpCenterService.createArticle(dto));
  }

  @Test
  void getArticles_Success() {
    when(dbService.findByQuery(
            "help_center",
            Map.of("selector", Map.of("category", "cat-1")),
            HelpCenterArticle.class))
        .thenReturn(List.of(article));

    List<ArticleResponseDTO> result = helpCenterService.getArticles("cat-1");

    assertEquals(1, result.size());
    assertEquals("article-1", result.getFirst().id());
  }

  @Test
  void getArticles_NullCategory_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> helpCenterService.getArticles(null));
  }

  @Test
  void getArticle_Success() {
    when(dbService.findById("help_center", "article-1", HelpCenterArticle.class))
        .thenReturn(article);
    when(editorService.getReadingTime(any())).thenReturn(5);

    FullArticleResponseDTO result = helpCenterService.getArticle("article-1");

    assertEquals("article-1", result.id());
    assertEquals(5, result.readingTimeInMinutes());
  }

  @Test
  void getArticle_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> helpCenterService.getArticle(null));
  }

  @Test
  void getArticle_NotFound_ThrowsNotFound() {
    when(dbService.findById("help_center", "non-existent", HelpCenterArticle.class))
        .thenReturn(null);

    assertThrows(NotFoundException.class, () -> helpCenterService.getArticle("non-existent"));
  }

  @Test
  void updateArticle_Success() {
    UpdateArticleRequestDTO request =
        new UpdateArticleRequestDTO("article-1", "1-abc", "Updated Title", List.of());
    when(dbService.findById("help_center", "article-1", HelpCenterArticle.class))
        .thenReturn(article);
    when(dbService.update("help_center", "article-1", article)).thenReturn("2-def");

    String result = helpCenterService.updateArticle(request, null);

    assertEquals("2-def", result);
    verify(sseService).sendRefresh("HELP_CENTER");
  }

  @Test
  void updateArticle_NotFound_ThrowsNotFound() {
    UpdateArticleRequestDTO request =
        new UpdateArticleRequestDTO("non-existent", "1-abc", "Title", List.of());
    when(dbService.findById("help_center", "non-existent", HelpCenterArticle.class))
        .thenReturn(null);

    assertThrows(NotFoundException.class, () -> helpCenterService.updateArticle(request, null));
  }

  @Test
  void verifyAssetOwnership_Success() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.IMAGE);
    block.setData("file-1.png");
    article.setContents(List.of(block));
    when(dbService.findById("help_center", "article-1", HelpCenterArticle.class))
        .thenReturn(article);
    when(editorService.extractFileIds(any())).thenReturn(java.util.Set.of("file-1.png"));

    assertDoesNotThrow(() -> helpCenterService.verifyAssetOwnership("article-1", "file-1.png"));
  }

  @Test
  void verifyAssetOwnership_NotLinked_ThrowsBadRequest() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.IMAGE);
    block.setData("file-1.png");
    article.setContents(List.of(block));
    when(dbService.findById("help_center", "article-1", HelpCenterArticle.class))
        .thenReturn(article);
    when(editorService.extractFileIds(any())).thenReturn(java.util.Set.of("file-1.png"));

    assertThrows(
        BadRequestException.class,
        () -> helpCenterService.verifyAssetOwnership("article-1", "file-2.png"));
  }

  @Test
  void deleteArticle_Success() {
    when(dbService.findById("help_center", "article-1", HelpCenterArticle.class))
        .thenReturn(article);
    when(appSettingsService.appSettings(ErrorAction.DELETE, ErrorResource.HELP_CENTER_ARTICLE))
        .thenReturn(appSettings);

    helpCenterService.deleteArticle("article-1");

    verify(dbService).delete("help_center", "article-1", "1-abc");
    verify(editorService).purgeAllBlockAssets(any());
    verify(sseService).sendRefresh("HELP_CENTER");
  }

  @Test
  void deleteArticle_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> helpCenterService.deleteArticle(null));
  }

  @Test
  void deleteArticle_NotFound_ThrowsNotFound() {
    when(dbService.findById("help_center", "non-existent", HelpCenterArticle.class))
        .thenReturn(null);

    assertThrows(NotFoundException.class, () -> helpCenterService.deleteArticle("non-existent"));
  }

  @Test
  void searchArticles_Success() {
    when(dbService.findAll("help_center", HelpCenterArticle.class)).thenReturn(List.of(article));
    when(editorService.deepSearch(anyList(), anyString())).thenReturn(List.of());

    var result = helpCenterService.searchArticles("test");

    assertNotNull(result);
  }

  @Test
  void searchArticles_NullTerm_ReturnsAll() {
    when(dbService.findAll("help_center", HelpCenterArticle.class)).thenReturn(List.of(article));

    var result = helpCenterService.searchArticles(null);

    assertEquals(1, result.size());
  }
}
