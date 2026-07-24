package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddReportRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDocumentAttachmentsDTO;
import com.gvw.gvwbackend.dto.request.UpdateReportDescriptionRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateReportRequestDTO;
import com.gvw.gvwbackend.dto.response.FullReportResponseDTO;
import com.gvw.gvwbackend.dto.response.ReportsResponseDTO;
import com.gvw.gvwbackend.dto.response.ReportsSearchResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.model.Report;
import com.gvw.gvwbackend.service.DbService;
import com.gvw.gvwbackend.service.FileValidator;
import com.gvw.gvwbackend.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/report")
public class ReportController {
  private final ReportService reportService;
  private final FileValidator fileValidator;
  private final DbService dbService;

  public ReportController(
      ReportService reportService, FileValidator fileValidator, DbService dbService) {
    this.reportService = reportService;
    this.fileValidator = fileValidator;
    this.dbService = dbService;
  }

  @GetMapping("/all")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  public ReportsResponseDTO getReports() {
    return reportService.getReports();
  }

  @GetMapping("/check/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  @ResponseStatus(HttpStatus.OK)
  public void checkReport(@PathVariable String id) {
    reportService.checkReport(id);
  }

  @PostMapping("/add")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  @ErrorContext(
      domain = ErrorDomain.REPORT,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  @ResponseStatus(HttpStatus.OK)
  public void addReport(@Valid @RequestBody AddReportRequestDTO request) {
    reportService.createReport(request);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  public FullReportResponseDTO getReport(@PathVariable String id) {
    return reportService.getReport(id);
  }

  @GetMapping("/search")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  public ReportsSearchResponseDTO reportsDeepSearch(@RequestParam("q") String input) {
    return reportService.reportDeepSearch(input);
  }

  @DeleteMapping("/delete/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  @ResponseStatus(HttpStatus.OK)
  public void deleteReport(@PathVariable String id) {
    reportService.deleteReport(id);
  }

  @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @ErrorContext(
      domain = ErrorDomain.REPORT,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  public Map<String, Object> updateReport(
      @RequestPart("reportData") @Valid UpdateReportRequestDTO request,
      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
    if (files != null) {
      for (MultipartFile file : files) {
        if (!fileValidator.isSafe(file)) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.FILE_VALIDATOR.createCode(ErrorAction.UTILITY, 400)));
        }
      }
    }
    String rev = reportService.updateReport(request, files);
    return Map.of("rev", rev);
  }

  @PatchMapping("/update/description")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  @ErrorContext(
      domain = ErrorDomain.TEXT_EDITOR,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateReportDescription(
      @Valid @RequestBody UpdateReportDescriptionRequestDTO request) {
    String rev = reportService.updateReportDescription(request);
    return Map.of("rev", rev);
  }

  @PatchMapping(
      value = "/{reportId}/update/attachments",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'SECRETARY')")
  @ErrorContext(
      domain = ErrorDomain.REPORT,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateReportAttachments(
      @PathVariable String reportId,
      @RequestPart("metadata") @Valid UpdateDocumentAttachmentsDTO request,
      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
    if (files != null) {
      for (MultipartFile file : files) {
        if (!fileValidator.isSafe(file)) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.FILE_VALIDATOR.createCode(ErrorAction.UTILITY, 400)));
        }
      }
    }

    String rev = reportService.updateAttachments(request, files, reportId);
    return Map.of("rev", rev);
  }

  @GetMapping("/{id}/attachments")
  public ResponseEntity<StreamingResponseBody> downloadScoreFiles(@PathVariable String id) {
    Report report = dbService.findById("reports", id, Report.class);
    if (report == null)
      throw new NotFoundException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UTILITY, 404)));
    if (report.getAttachments() == null || report.getAttachments().isEmpty()) {
      return ResponseEntity.noContent().build();
    }

    String sanitizedTitle = report.getTitle().replaceAll("[\"\r\n]", "_");

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/zip")
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedTitle + ".zip\"")
        .body(out -> reportService.streamFilesAsZip(report.getAttachments(), out));
  }
}
