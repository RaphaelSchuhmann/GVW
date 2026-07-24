package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.*;
import com.gvw.gvwbackend.dto.response.ArticlesResponseDTO;
import com.gvw.gvwbackend.dto.response.ArticlesSearchResponseDTO;
import com.gvw.gvwbackend.dto.response.FullArticleResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.AppSettingsService;
import com.gvw.gvwbackend.service.FileValidator;
import com.gvw.gvwbackend.service.HelpCenterService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/help")
public class HelpCenterController {
  private final AppSettingsService appSettingsService;
  private final HelpCenterService helpCenterService;
  private final FileValidator fileValidator;

  public HelpCenterController(
      AppSettingsService appSettingsService,
      HelpCenterService helpCenterService,
      FileValidator fileValidator) {
    this.appSettingsService = appSettingsService;
    this.helpCenterService = helpCenterService;
    this.fileValidator = fileValidator;
  }

  @PostMapping("/category/add")
  @ErrorContext(
      domain = ErrorDomain.HELP_CENTER,
      action = ErrorAction.CREATE,
      resource = ErrorResource.HELP_CENTER_CATEGORY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public Map<String, String> addCategory(
      @Valid @RequestBody AddHelpCenterCategoryRequestDTO request) {
    String rev = appSettingsService.addHelpCenterCategoryToSettings(request);
    return Map.of("rev", rev);
  }

  @DeleteMapping("/category/delete/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public Map<String, String> removeCategory(@PathVariable String id) {
    String rev = helpCenterService.removeHelpCenterCategory(id);
    return Map.of("rev", rev);
  }

  @PatchMapping("/category/update/featured")
  @ErrorContext(
      domain = ErrorDomain.HELP_CENTER,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.HELP_CENTER_CATEGORY)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public Map<String, String> featureCategories(
      @Valid @RequestBody SetFeaturedHelpCenterCategoriesRequestDTO request) {
    String rev = appSettingsService.updateFeaturedHelpCenterCategories(request);
    return Map.of("rev", rev);
  }

  @GetMapping("/category/check/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void checkCategory(@PathVariable String id) {
    helpCenterService.categoryExists(id);
  }

  @PostMapping("/article/add")
  @ErrorContext(
      domain = ErrorDomain.HELP_CENTER,
      action = ErrorAction.CREATE,
      resource = ErrorResource.HELP_CENTER_ARTICLE)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public Map<String, String> addArticle(
      @Valid @RequestBody AddHelpCenterArticleRequestDTO request) {
    String rev = helpCenterService.createArticle(request);
    return Map.of("rev", rev);
  }

  @GetMapping("/article/get")
  public ArticlesResponseDTO getArticlesOfCategory(@RequestParam("category") String category) {
    return helpCenterService.getArticles(category);
  }

  @GetMapping("/article/check/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void checkArticle(@PathVariable String id) {
    helpCenterService.articleExists(id);
  }

  @GetMapping("/article/{id}")
  @ResponseStatus(HttpStatus.OK)
  public FullArticleResponseDTO getFullArticle(@PathVariable String id) {
    return helpCenterService.getArticle(id);
  }

  @PatchMapping(value = "/article/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ErrorContext(
      domain = ErrorDomain.HELP_CENTER,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public Map<String, Object> updateArticle(
      @RequestPart("articleData") @Valid UpdateArticleRequestDTO request,
      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
    if (files != null) {
      for (MultipartFile file : files) {
        if (!fileValidator.isSafe(file)) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.FILE_VALIDATOR.createCode(ErrorAction.UTILITY, 400)));
        }
      }
    }
    String rev = helpCenterService.updateArticle(request, files);
    return Map.of("rev", rev);
  }

  @DeleteMapping("/article/delete/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN')")
  public void deleteArticle(@PathVariable String id) {
    helpCenterService.deleteArticle(id);
  }

  @GetMapping("/article/search")
  @ResponseStatus(HttpStatus.OK)
  public Map<String, List<ArticlesSearchResponseDTO>> searchArticles(
      @RequestParam(name = "term") String searchTerm) {
    return Map.of("data", helpCenterService.searchArticles(searchTerm));
  }
}
