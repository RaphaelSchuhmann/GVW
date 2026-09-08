package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddBugReportRequestDTO;
import com.gvw.gvwbackend.dto.response.BugReportDetailsResponseDTO;
import com.gvw.gvwbackend.dto.response.BugReportResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.BugReport;
import com.gvw.gvwbackend.model.UserReportMetaData;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BugReportServiceTest {

  @Mock private DbService dbService;

  @Mock private SseService sseService;

  @Mock private UserService userService;

  @Mock private MailService mailService;

  @InjectMocks private BugReportService bugReportService;

  private BugReport bugReport;

  @BeforeEach
  void setUp() {
    UserReportMetaData metaData = new UserReportMetaData();
    metaData.setUserId("user-1");
    metaData.setTimestamp(LocalDateTime.now());
    metaData.setAppVersion("1.0.0");
    metaData.setRoute("/test");
    metaData.setOs("Windows");
    metaData.setBrowser("Chrome");
    metaData.setViewport("1920x1080");

    bugReport = new BugReport();
    bugReport.setId("bug-1");
    bugReport.setTitle("Test Bug");
    bugReport.setSeverity("high");
    bugReport.setStepsToReproduce("Step 1, Step 2");
    bugReport.setMetaData(metaData);
  }

  @Test
  void getBugReports_Success() {
    when(dbService.findAll("bug_reports", BugReport.class)).thenReturn(List.of(bugReport));

    List<BugReportResponseDTO> result = bugReportService.getBugReports();

    assertEquals(1, result.size());
    assertEquals("bug-1", result.getFirst().id());
    assertEquals("Test Bug", result.getFirst().title());
    assertEquals("high", result.getFirst().severity());
  }

  @Test
  void getBugReports_Empty_ReturnsEmptyList() {
    when(dbService.findAll("bug_reports", BugReport.class)).thenReturn(List.of());

    List<BugReportResponseDTO> result = bugReportService.getBugReports();

    assertTrue(result.isEmpty());
  }

  @Test
  void getBugReportDetails_Success() {
    when(dbService.findById("bug_reports", "bug-1", BugReport.class)).thenReturn(bugReport);
    when(userService.resolveUserIdToEmail("user-1")).thenReturn("test@example.com");

    BugReportDetailsResponseDTO result = bugReportService.getBugReportDetails("bug-1");

    assertEquals("Test Bug", result.title());
    assertEquals("high", result.severity());
    assertEquals("Step 1, Step 2", result.stepsToReproduce());
    assertEquals("test@example.com", result.userEmail());
  }

  @Test
  void getBugReportDetails_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> bugReportService.getBugReportDetails(null));
  }

  @Test
  void getBugReportDetails_BlankId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> bugReportService.getBugReportDetails(""));
  }

  @Test
  void getBugReportDetails_NotFound_ThrowsNotFound() {
    when(dbService.findById("bug_reports", "non-existent", BugReport.class)).thenReturn(null);

    assertThrows(
        NotFoundException.class, () -> bugReportService.getBugReportDetails("non-existent"));
  }

  @Test
  void addBugReport_Success() {
    AddBugReportRequestDTO request =
        new AddBugReportRequestDTO(
            "New Bug", "high", "Steps", "/test", "1.0.0", "Windows", "Chrome", "1920x1080");
    when(userService.resolveUserIdToEmail("user-1")).thenReturn("test@example.com");

    bugReportService.addBugReport(request, "user-1");

    verify(dbService).insert(eq("bug_reports"), any(BugReport.class));
    verify(mailService).sendMail(eq("test@example.com"), anyString(), anyString(), anyMap());
    verify(sseService).sendRefresh("BUG");
  }

  @Test
  void addBugReport_NullUserId_ThrowsBadRequest() {
    AddBugReportRequestDTO request =
        new AddBugReportRequestDTO(
            "New Bug", "high", "Steps", "/test", "1.0.0", "Windows", "Chrome", "1920x1080");

    assertThrows(BadRequestException.class, () -> bugReportService.addBugReport(request, null));
  }

  @Test
  void addBugReport_BlankUserId_ThrowsBadRequest() {
    AddBugReportRequestDTO request =
        new AddBugReportRequestDTO(
            "New Bug", "high", "Steps", "/test", "1.0.0", "Windows", "Chrome", "1920x1080");

    assertThrows(BadRequestException.class, () -> bugReportService.addBugReport(request, ""));
  }

  @Test
  void addBugReport_MailFailure_LogsWarning() {
    AddBugReportRequestDTO request =
        new AddBugReportRequestDTO(
            "New Bug", "high", "Steps", "/test", "1.0.0", "Windows", "Chrome", "1920x1080");
    when(userService.resolveUserIdToEmail("user-1")).thenReturn("test@example.com");
    doThrow(new RuntimeException("Mail error"))
        .when(mailService)
        .sendMail(anyString(), anyString(), anyString(), anyMap());

    assertDoesNotThrow(() -> bugReportService.addBugReport(request, "user-1"));
    verify(sseService).sendRefresh("BUG");
  }

  @Test
  void deleteBugReport_Success() {
    when(dbService.findById("bug_reports", "bug-1", BugReport.class)).thenReturn(bugReport);

    bugReportService.deleteBugReport("bug-1");

    verify(dbService).delete("bug_reports", "bug-1", bugReport.getRev());
    verify(sseService).sendRefresh("BUG");
  }

  @Test
  void deleteBugReport_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> bugReportService.deleteBugReport(null));
  }

  @Test
  void deleteBugReport_BlankId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> bugReportService.deleteBugReport(""));
  }

  @Test
  void deleteBugReport_NotFound_ThrowsNotFound() {
    when(dbService.findById("bug_reports", "non-existent", BugReport.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> bugReportService.deleteBugReport("non-existent"));
  }
}
