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

/**
 * Service responsible for managing user-submitted bug reports.
 *
 * <p>Bug reports are stored in CouchDB and contain additional metadata about the client that
 * submitted the report, such as browser, operating system, application version, and viewport size.
 *
 * <p>Creating or deleting reports broadcasts SSE refresh events so connected clients can update
 * their data automatically.
 */
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

  /**
   * Retrieves all bug reports from the database.
   *
   * <p>Only summary information is returned for each report. Detailed information including
   * reproduction steps and client metadata is loaded separately through {@link
   * #getBugReportDetails(String)}.
   *
   * @return list of bug report summaries
   */
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

  /**
   * Retrieves the complete information of a single bug report.
   *
   * <p>The stored user ID is resolved into an email address before returning the response to
   * provide administrators with the reporter information.
   *
   * @param id database ID of the bug report
   * @return detailed bug report information
   */
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

  /**
   * Creates a new bug report.
   *
   * <p>The report is stored together with metadata describing the client environment. After
   * successful creation:
   *
   * <ul>
   *   <li>The reporting user receives an email confirmation.
   *   <li>Connected clients are notified through SSE.
   * </ul>
   *
   * <p>Email failures are intentionally ignored because a failed notification should not prevent a
   * valid bug report from being stored.
   *
   * @param request submitted bug report data
   * @param userId ID of the user creating the report
   */
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
      log.warn("Failed to broadcast BUG refresh", ex);
    }
  }

  /**
   * Permanently deletes a bug report from the database.
   *
   * <p>After deletion, connected clients receive an SSE refresh event.
   *
   * @param id database ID of the bug report to remove
   */
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
      log.warn("Failed to broadcast BUG refresh", ex);
    }
  }
}
