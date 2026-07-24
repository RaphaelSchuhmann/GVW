package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddHelpCenterArticleRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateArticleRequestDTO;
import com.gvw.gvwbackend.dto.response.*;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
public class HelpCenterService {
  private final DbService dbService;
  private final SseService sseService;
  private final AppSettingsService appSettingsService;
  private final TextEditorService editorService;
  private final ObjectMapper mapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(HelpCenterService.class);
  private static final long MAX_FILE_SIZE = 8 * 1024 * 1024;

  public HelpCenterService(
      DbService dbService,
      SseService sseService,
      AppSettingsService appSettingsService,
      TextEditorService editorService) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.appSettingsService = appSettingsService;
    this.editorService = editorService;
  }

  public void categoryExists(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.CHECK, 400, ErrorResource.HELP_CENTER_CATEGORY)));
    }

    AppSettings settings =
        appSettingsService.appSettings(ErrorAction.CHECK, ErrorResource.HELP_CENTER_CATEGORY);
    List<HelpCenterCategory> categories = settings.getHelpCenterCategories();

    if (categories == null || categories.stream().noneMatch(obj -> obj.getId().equals(id))) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.CHECK, 404, ErrorResource.HELP_CENTER_CATEGORY)));
    }
  }

  public void articleExists(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.CHECK, 400, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    HelpCenterArticle article = dbService.findById("help_center", id, HelpCenterArticle.class);
    if (article == null) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.CHECK, 404, ErrorResource.HELP_CENTER_ARTICLE)));
    }
  }

  public String removeHelpCenterCategory(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.HELP_CENTER.createCode(ErrorAction.DELETE, 400)));
    }

    List<HelpCenterArticle> articles =
        dbService.findByQuery(
            "help_center", Map.of("selector", Map.of("category", id)), HelpCenterArticle.class);
    if (!articles.isEmpty()) {
      throw new ConflictException(
          String.valueOf(ErrorDomain.HELP_CENTER.createCode(ErrorAction.DELETE, 409)));
    }

    return appSettingsService.removeHelpCenterCategoryFromSettings(id);
  }

  public String createArticle(AddHelpCenterArticleRequestDTO dto) {
    // Check if category is valid
    AppSettings settings =
        appSettingsService.appSettings(ErrorAction.CREATE, ErrorResource.HELP_CENTER_ARTICLE);
    List<HelpCenterCategory> categories = settings.getHelpCenterCategories();

    HelpCenterCategory category = null;
    if (categories != null) {
      category =
          categories.stream()
              .filter(obj -> obj.getId().equals(dto.category()))
              .findFirst()
              .orElse(null);
    }

    if (category == null) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.CREATE, 400, ErrorResource.HELP_CENTER_CATEGORY)));
    }

    HelpCenterArticle article =
        HelpCenterArticle.builder()
            .title(dto.title())
            .description(dto.description())
            .category(dto.category())
            .tags(dto.tags())
            .build();

    TextEditorBlock startBlock = new TextEditorBlock();
    startBlock.setId(UUID.randomUUID().toString());
    startBlock.setType(TextEditorBlockType.TEXT);
    startBlock.setData("");
    article.setContents(List.of(startBlock));

    dbService.insert("help_center", article);

    String rev =
        appSettingsService.updateHelpCenterCategoryArticleCount(
            dto.category(), category.getArticleCount() + 1);

    try {
      sseService.broadcastRefresh("HELP_CENTER");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast HELP_CENTER refresh: ", ex);
    }

    return rev;
  }

  public ArticlesResponseDTO getArticles(String category) {
    if (category == null || category.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.READ_ALL, 400, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    List<HelpCenterArticle> articles =
        dbService.findByQuery(
            "help_center",
            Map.of("selector", Map.of("category", category)),
            HelpCenterArticle.class);

    if (articles == null || articles.isEmpty()) {
      return new ArticlesResponseDTO(List.of());
    }

    List<ArticleResponseDTO> responseDTOs =
        articles.stream()
            .map(
                m ->
                    new ArticleResponseDTO(
                        m.getId(), m.getTitle(), m.getDescription(), m.getTags()))
            .toList();
    return new ArticlesResponseDTO(responseDTOs);
  }

  public FullArticleResponseDTO getArticle(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.READ_ONE, 400, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    HelpCenterArticle article = dbService.findById("help_center", id, HelpCenterArticle.class);
    if (article == null) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.READ_ONE, 404, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    return new FullArticleResponseDTO(
        article.getId(),
        article.getRev(),
        article.getTitle(),
        article.getDescription(),
        article.getContents(),
        editorService.getReadingTime(article.getContents()));
  }

  public String updateArticle(UpdateArticleRequestDTO request, List<MultipartFile> files) {
    HelpCenterArticle article =
        dbService.findById("help_center", request.id(), HelpCenterArticle.class);
    if (article == null) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.UPDATE, 404, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    List<TextEditorBlock> oldContents = article.getContents();
    Map<String, String> newlyUploadedFiles = new HashMap<>();

    try {
      if (files != null && !files.isEmpty()) {
        newlyUploadedFiles = editorService.processUploadedFiles(files, ErrorAction.UPDATE);
      }

      // Update Image block data to permanent internal filenames
      List<TextEditorBlock> blocks = request.content();
      for (TextEditorBlock block : blocks) {
        if (block.getType() != TextEditorBlockType.IMAGE) continue;

        String tempId = block.getData();
        if (tempId.startsWith("temp_")) {
          String realId = newlyUploadedFiles.get(tempId);
          if (realId != null) {
            block.setData(realId);
          } else {
            log.error("Missing file for temp ID: {}", tempId);
            throw new BadRequestException(
                String.valueOf(
                    ErrorDomain.HELP_CENTER.createCode(
                        ErrorAction.UPDATE, 400, ErrorResource.HELP_CENTER_ARTICLE)));
          }
        }
      }

      article.setTitle(request.title());
      article.setContents(request.content());
      article.setRev(request.rev());

      Map<String, Object> resp = dbService.update("help_center", article.getId(), article);

      if (resp == null || !resp.containsKey("rev")) {
        throw new RuntimeException(
            String.valueOf(ErrorDomain.HELP_CENTER.createCode(ErrorAction.UPDATE, 500)));
      }

      editorService.synchronizeBlockAssets(oldContents, request.content(), ErrorAction.UPDATE);

      try {
        sseService.broadcastRefresh("HELP_CENTER");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast HELP_CENTER refresh: ", ex);
      }

      return (String) resp.get("rev");
    } catch (Exception e) {
      log.error("Update failed for article ID: {}", request.id(), e);

      if (e instanceof BadRequestException) throw (BadRequestException) e;
      if (e instanceof NotFoundException) throw (NotFoundException) e;

      throw new RuntimeException(
          String.valueOf(ErrorDomain.HELP_CENTER.createCode(ErrorAction.UPDATE, 500)), e);
    }
  }

  public void verifyAssetOwnership(String documentId, String filename) {
    if (documentId == null || documentId.isBlank() || filename == null || filename.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 400)));
    }

    HelpCenterArticle article =
        dbService.findById("help_center", documentId, HelpCenterArticle.class);
    if (article == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 404)));
    }

    Set<String> linkedFileIds = editorService.extractFileIds(article.getContents());

    if (!linkedFileIds.contains(filename)) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 400)));
    }
  }

  public void deleteArticle(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.DELETE, 400, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    HelpCenterArticle article = dbService.findById("help_center", id, HelpCenterArticle.class);
    if (article == null) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.HELP_CENTER.createCode(
                  ErrorAction.DELETE, 404, ErrorResource.HELP_CENTER_ARTICLE)));
    }

    AppSettings settings =
        appSettingsService.appSettings(ErrorAction.DELETE, ErrorResource.HELP_CENTER_ARTICLE);
    List<HelpCenterCategory> categories = settings.getHelpCenterCategories();

    HelpCenterCategory category =
        categories.stream()
            .filter(obj -> obj.getId().equals(article.getCategory()))
            .findFirst()
            .orElse(null);
    if (category != null) {
      appSettingsService.updateHelpCenterCategoryArticleCount(
          article.getCategory(), category.getArticleCount() - 1);
    }

    dbService.delete("help_center", article.getId(), article.getRev());

    editorService.purgeAllBlockAssets(article.getContents(), ErrorAction.DELETE);

    try {
      sseService.broadcastRefresh("HELP_CENTER");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast HELP_CENTER refresh: ", ex);
    }
  }

  public List<ArticlesSearchResponseDTO> searchArticles(String searchTerm) {
    List<Map<String, Object>> rawArticles = dbService.findAll("help_center");

    List<HelpCenterArticle> articles =
        rawArticles.stream().map(map -> mapper.convertValue(map, HelpCenterArticle.class)).toList();

    if (articles.isEmpty()) {
      return List.of();
    }

    // Fallback if no search term is provided
    if (searchTerm == null || searchTerm.isBlank()) {
      return articles.stream()
          .map(
              m ->
                  new ArticlesSearchResponseDTO(
                      m.getId(), m.getTitle(), m.getDescription(), m.getTags()))
          .toList();
    }

    Set<ArticlesSearchResponseDTO> combinedResults = new LinkedHashSet<>();
    String lowerSearchTerm = searchTerm.toLowerCase();

    for (HelpCenterArticle article : articles) {
      if (article.getTitle() != null
          && article.getTitle().toLowerCase().contains(lowerSearchTerm)) {
        combinedResults.add(
            new ArticlesSearchResponseDTO(
                article.getId(),
                article.getTitle(),
                article.getDescription(), // Fallback to standard description if matched on title
                article.getTags()));
      }
    }

    List<TextDocumentSearchResult<HelpCenterArticle>> deepSearchResults =
        editorService.deepSearch(articles, searchTerm);

    for (TextDocumentSearchResult<HelpCenterArticle> hit : deepSearchResults) {
      HelpCenterArticle doc = hit.getDocument();

      ArticlesSearchResponseDTO dto =
          new ArticlesSearchResponseDTO(
              doc.getId(),
              doc.getTitle(),
              hit.getSnippet(), // Use the highlighted search snippet
              doc.getTags());

      combinedResults.add(dto);
    }

    return new ArrayList<>(combinedResults);
  }
}
