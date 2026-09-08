package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddReportRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateReportDescriptionRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateReportRequestDTO;
import com.gvw.gvwbackend.dto.response.FullReportResponseDTO;
import com.gvw.gvwbackend.dto.response.ReportResponseDTO;
import com.gvw.gvwbackend.dto.response.ReportSearchResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.Report;
import com.gvw.gvwbackend.model.TextEditorBlock;
import com.gvw.gvwbackend.model.TextEditorBlockType;
import com.gvw.gvwbackend.util.FileUtils;
import java.io.OutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

  @Mock private DbService dbService;

  @Mock private SseService sseService;

  @Mock private TextEditorService editorService;

  @Mock private FileUtils fileUtils;

  @InjectMocks private ReportService reportService;

  private Report report;

  @BeforeEach
  void setUp() {
    report = new Report();
    report.setId("report-1");
    report.setRev("1-abc");
    report.setTitle("Test Report");
    report.setAuthor("John Doe");
    report.setDescription("Test description");
    report.setType("meeting");
    report.setCreatedAt(Instant.now().toString());
    report.setLastEditedBy("John Doe");
    report.setContents(List.of(new TextEditorBlock()));
    report.setAttachments(List.of());
  }

  @Test
  void getReports_Success() {
    when(dbService.findAll("reports", Report.class)).thenReturn(List.of(report));

    List<ReportResponseDTO> result = reportService.getReports();

    assertEquals(1, result.size());
    assertEquals("report-1", result.getFirst().id());
    assertEquals("Test Report", result.getFirst().title());
  }

  @Test
  void getReports_Empty_ReturnsEmptyList() {
    when(dbService.findAll("reports", Report.class)).thenReturn(List.of());

    List<ReportResponseDTO> result = reportService.getReports();

    assertTrue(result.isEmpty());
  }

  @Test
  void createReport_Success() {
    AddReportRequestDTO request =
        new AddReportRequestDTO("New Report", "Jane Doe", "desc", "meeting");

    reportService.createReport(request);

    verify(dbService).insert(eq("reports"), any(Report.class));
    verify(sseService).sendRefresh("REPORTS");
  }

  @Test
  void getReport_Success() {
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);
    when(editorService.convertBlocksToPlainText(any())).thenReturn("Test content");
    when(editorService.getReadingTime(any())).thenReturn(1);

    FullReportResponseDTO result = reportService.getReport("report-1");

    assertEquals("report-1", result.id());
    assertEquals("Test Report", result.title());
    assertEquals(1, result.readingTimeInMinutes());
  }

  @Test
  void getReport_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> reportService.getReport(null));
  }

  @Test
  void getReport_NotFound_ThrowsNotFound() {
    when(dbService.findById("reports", "non-existent", Report.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> reportService.getReport("non-existent"));
  }

  @Test
  void verifyAssetOwnership_Success() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.IMAGE);
    block.setData("file-1.png");
    report.setContents(List.of(block));
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);
    when(editorService.extractFileIds(any())).thenReturn(Set.of("file-1.png"));

    assertDoesNotThrow(() -> reportService.verifyAssetOwnership("report-1", "file-1.png"));
  }

  @Test
  void verifyAssetOwnership_NotLinked_ThrowsBadRequest() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.IMAGE);
    block.setData("file-1.png");
    report.setContents(List.of(block));
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);
    when(editorService.extractFileIds(any())).thenReturn(Set.of("file-1.png"));

    assertThrows(
        BadRequestException.class,
        () -> reportService.verifyAssetOwnership("report-1", "file-2.png"));
  }

  @Test
  void deleteReport_Success() {
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);

    reportService.deleteReport("report-1");

    verify(dbService).delete("reports", "report-1", "1-abc");
    verify(editorService).purgeAllBlockAssets(any());
    verify(sseService).sendRefresh("REPORTS");
  }

  @Test
  void deleteReport_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> reportService.deleteReport(null));
  }

  @Test
  void deleteReport_NotFound_ThrowsNotFound() {
    when(dbService.findById("reports", "non-existent", Report.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> reportService.deleteReport("non-existent"));
  }

  @Test
  void reportDeepSearch_Success() {
    when(dbService.findAll("reports", Report.class)).thenReturn(List.of(report));
    when(editorService.deepSearch(anyList(), anyString())).thenReturn(List.of());

    List<ReportSearchResponseDTO> result = reportService.reportDeepSearch("test");

    assertNotNull(result);
  }

  @Test
  void reportDeepSearch_NullInput_ReturnsEmptyList() {
    List<ReportSearchResponseDTO> result = reportService.reportDeepSearch(null);

    assertTrue(result.isEmpty());
  }

  @Test
  void updateReport_Success() {
    UpdateReportRequestDTO request =
        new UpdateReportRequestDTO("report-1", "1-abc", "Updated Title", "Jane Doe", List.of());
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);
    when(dbService.update("reports", "report-1", report)).thenReturn("2-def");

    String result = reportService.updateReport(request, null);

    assertEquals("2-def", result);
    verify(sseService).sendRefresh("REPORTS");
  }

  @Test
  void updateReport_NotFound_ThrowsNotFound() {
    UpdateReportRequestDTO request =
        new UpdateReportRequestDTO("non-existent", "1-abc", "Title", "Author", List.of());
    when(dbService.findById("reports", "non-existent", Report.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> reportService.updateReport(request, null));
  }

  @Test
  void updateReportDescription_Success() {
    UpdateReportDescriptionRequestDTO request =
        new UpdateReportDescriptionRequestDTO("report-1", "1-abc", "New description");
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);
    when(dbService.update("reports", "report-1", report)).thenReturn("2-def");

    String result = reportService.updateReportDescription(request);

    assertEquals("2-def", result);
    assertEquals("New description", report.getDescription());
    verify(sseService).sendRefresh("REPORTS");
  }

  @Test
  void updateReportDescription_BlankDescription_UsesDefault() {
    UpdateReportDescriptionRequestDTO request =
        new UpdateReportDescriptionRequestDTO("report-1", "1-abc", "");
    when(dbService.findById("reports", "report-1", Report.class)).thenReturn(report);
    when(dbService.update("reports", "report-1", report)).thenReturn("2-def");

    String result = reportService.updateReportDescription(request);

    assertEquals("2-def", result);
    assertEquals("Keine Beschreibung", report.getDescription());
  }

  @Test
  void streamFilesAsZip_Success() {
    OutputStream out = mock(OutputStream.class);

    assertDoesNotThrow(() -> reportService.streamFilesAsZip(report.getAttachments(), out));
    verify(fileUtils)
        .streamFilesAsZip(eq(report.getAttachments()), isNull(), eq(out), eq(ErrorDomain.REPORT));
  }
}
