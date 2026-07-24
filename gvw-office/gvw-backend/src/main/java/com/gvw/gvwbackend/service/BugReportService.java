package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddBugReportRequestDTO;
import com.gvw.gvwbackend.dto.response.BugReportDetailsResponseDTO;
import com.gvw.gvwbackend.dto.response.BugReportResponseDTO;
import com.gvw.gvwbackend.dto.response.BugReportsResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.BugReport;
import com.gvw.gvwbackend.model.UserReportMetaData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class BugReportService {
  private final DbService dbService;
  private final SseService sseService;
  private final ObjectMapper mapper = new ObjectMapper();
  private final UserService userService;
  private final MailService mailService;
  private static final Logger log = LoggerFactory.getLogger(BugReportService.class);

  public BugReportService(
      DbService dbService,
      SseService sseService,
      UserService userService,
      MailService mailService) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.userService = userService;
    this.mailService = mailService;
  }

  public BugReportsResponseDTO getBugReports() {
    List<Map<String, Object>> rawBugReports = dbService.findAll("bug_reports");

    List<BugReport> bugReports =
        rawBugReports.stream().map(map -> mapper.convertValue(map, BugReport.class)).toList();

    if (bugReports.isEmpty()) {
      return new BugReportsResponseDTO(List.of());
    }

    List<BugReportResponseDTO> bugReportResponseDTOS =
        bugReports.stream()
            .map(m -> new BugReportResponseDTO(m.getId(), m.getTitle(), m.getSeverity()))
            .toList();

    return new BugReportsResponseDTO(bugReportResponseDTOS);
  }

  public BugReportDetailsResponseDTO getBugReportDetails(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.BUG_REPORT.createCode(ErrorAction.READ_ONE, 400)));
    }

    BugReport bugReport = dbService.findById("bug_reports", id, BugReport.class);
    if (bugReport == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.BUG_REPORT.createCode(ErrorAction.READ_ONE, 404)));
    }

    return new BugReportDetailsResponseDTO(
        bugReport.getTitle(),
        bugReport.getSeverity(),
        bugReport.getStepsToReproduce(),
        userService.resolveUserIdToEmail(bugReport.getMetaData().getUserId()),
        bugReport.getMetaData().getTimestamp(),
        bugReport.getMetaData().getAppVersion(),
        bugReport.getMetaData().getRoute(),
        bugReport.getMetaData().getOs(),
        bugReport.getMetaData().getBrowser(),
        bugReport.getMetaData().getViewport());
  }

  public void addBugReport(AddBugReportRequestDTO request, String userId) {
    if (userId == null || userId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.BUG_REPORT.createCode(ErrorAction.CREATE, 400)));
    }

    BugReport bugReport = new BugReport();
    bugReport.setTitle(request.title());
    bugReport.setSeverity(request.severity());
    bugReport.setStepsToReproduce(request.stepsToReproduce());

    UserReportMetaData metaData = new UserReportMetaData();
    metaData.setUserId(userId);
    metaData.setRoute(request.route());
    metaData.setAppVersion(request.appVersion());
    metaData.setTimestamp(LocalDateTime.now());
    metaData.setOs(request.os());
    metaData.setBrowser(request.browser());
    metaData.setViewport(request.viewport());

    bugReport.setMetaData(metaData);

    dbService.insert("bug_reports", bugReport);

    String email = userService.resolveUserIdToEmail(userId);

    if (!email.isBlank()) {
      try {
        mailService.sendMail(email, "GVW-Office: Neuer Bug gemeldet", "newBug", Map.of());
      } catch (RuntimeException ex) {
        log.warn("Failed to send new bug notification email: ", ex);
      }
    }

    try {
      sseService.broadcastRefresh("BUG");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast BUG refresh: ", ex);
    }
  }

  public void deleteBugReport(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.BUG_REPORT.createCode(ErrorAction.DELETE, 400)));
    }

    BugReport bugReport = dbService.findById("bug_reports", id, BugReport.class);
    if (bugReport == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.BUG_REPORT.createCode(ErrorAction.DELETE, 404)));
    }

    boolean deleted = dbService.delete("bug_reports", bugReport.getId(), bugReport.getRev());
    if (!deleted) {
      throw new RuntimeException(
          String.valueOf(ErrorDomain.BUG_REPORT.createCode(ErrorAction.DELETE, 500)));
    }

    try {
      sseService.broadcastRefresh("BUG");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast BUG refresh: ", ex);
    }
  }
}
