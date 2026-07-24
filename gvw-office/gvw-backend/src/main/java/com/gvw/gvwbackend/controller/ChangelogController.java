package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddChangelogRequestDTO;
import com.gvw.gvwbackend.dto.response.ChangelogsResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.ChangelogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/changelogs")
public class ChangelogController {
  private final ChangelogService changelogService;

  public ChangelogController(ChangelogService changelogService) {
    this.changelogService = changelogService;
  }

  @GetMapping("/all")
  public ChangelogsResponseDTO getChangelogs() {
    return changelogService.getChangelogs();
  }

  @PostMapping("/add")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  @ErrorContext(
      domain = ErrorDomain.CHANGELOG,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void addChangelog(@Valid @RequestBody AddChangelogRequestDTO request) {
    changelogService.addChangelog(request);
  }

  @DeleteMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void deleteChangelog(@PathVariable String id) {
    changelogService.deleteChangelog(id);
  }
}
