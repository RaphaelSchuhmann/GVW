package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddBugReportRequestDTO;
import com.gvw.gvwbackend.dto.request.AddFeedbackRequestDTO;
import com.gvw.gvwbackend.dto.response.BugReportDetailsResponseDTO;
import com.gvw.gvwbackend.dto.response.BugReportsResponseDTO;
import com.gvw.gvwbackend.dto.response.FeedbackDetailsResponseDTO;
import com.gvw.gvwbackend.dto.response.FeedbacksResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.BugReportService;
import com.gvw.gvwbackend.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/support")
public class SupportController {
  private final FeedbackService feedbackService;
  private final BugReportService bugReportService;

  public SupportController(FeedbackService feedbackService, BugReportService bugReportService) {
    this.feedbackService = feedbackService;
    this.bugReportService = bugReportService;
  }

  @GetMapping("/feedback/all")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public FeedbacksResponseDTO getFeedbacks() {
    return feedbackService.getFeedbacks();
  }

  @GetMapping("/bugs/all")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public BugReportsResponseDTO getBugReports() {
    return bugReportService.getBugReports();
  }

  @GetMapping("/feedback/details/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public FeedbackDetailsResponseDTO getFeedbackDetails(@PathVariable String id) {
    return feedbackService.getFeedbackDetails(id);
  }

  @GetMapping("/bugs/details/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  public BugReportDetailsResponseDTO getBugReportDetails(@PathVariable String id) {
    return bugReportService.getBugReportDetails(id);
  }

  @PostMapping("/feedback/add")
  @ResponseStatus(HttpStatus.OK)
  @ErrorContext(
      domain = ErrorDomain.FEEDBACK,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void addFeedback(
      @Valid @RequestBody AddFeedbackRequestDTO request,
      @RequestAttribute("userId") String userId) {
    feedbackService.addFeedback(request, userId);
  }

  @PostMapping("/bugs/add")
  @ResponseStatus(HttpStatus.OK)
  @ErrorContext(
      domain = ErrorDomain.BUG_REPORT,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void addBugReport(
      @Valid @RequestBody AddBugReportRequestDTO request,
      @RequestAttribute("userId") String userId) {
    bugReportService.addBugReport(request, userId);
  }

  @DeleteMapping("/feedback/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void deleteFeedback(@PathVariable String id) {
    feedbackService.deleteFeedback(id);
  }

  @DeleteMapping("/bugs/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN')")
  @ResponseStatus(HttpStatus.OK)
  public void deleteBugReport(@PathVariable String id) {
    bugReportService.deleteBugReport(id);
  }
}
