package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddCategoryRequestDTO;
import com.gvw.gvwbackend.dto.request.RemoveCategoryRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateMaxMembersRequestDTO;
import com.gvw.gvwbackend.dto.response.AppSettingsResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.AppSettingsService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
public class AppSettingsController {
  private final AppSettingsService appSettingsService;

  public AppSettingsController(AppSettingsService appSettingsService) {
    this.appSettingsService = appSettingsService;
  }

  @GetMapping("/get")
  public AppSettingsResponseDTO getAppSettings() {
    return appSettingsService.getAppSettings();
  }

  @PatchMapping("/update/max-members")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.APP_SETTINGS,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateMaxMembers(
      @Valid @RequestBody UpdateMaxMembersRequestDTO requestDTO) {
    String rev = appSettingsService.updateMaxMembers(requestDTO);
    return Map.of("rev", rev);
  }

  @PostMapping("/add/category")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'LIBRARIAN', 'CONDUCTOR')")
  @ErrorContext(
      domain = ErrorDomain.APP_SETTINGS,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.LIBRARY_CATEGORY)
  public Map<String, Object> addCategory(@Valid @RequestBody AddCategoryRequestDTO requestDTO) {
    String rev = appSettingsService.addCategory(requestDTO);
    return Map.of("rev", rev);
  }

  @PostMapping("/remove/category")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'LIBRARIAN', 'CONDUCTOR')")
  @ErrorContext(
      domain = ErrorDomain.APP_SETTINGS,
      action = ErrorAction.DELETE,
      resource = ErrorResource.LIBRARY_CATEGORY)
  public Map<String, Object> removeCategory(
      @Valid @RequestBody RemoveCategoryRequestDTO requestDTO) {
    String rev = appSettingsService.removeCategory(requestDTO);
    return Map.of("rev", rev);
  }
}
