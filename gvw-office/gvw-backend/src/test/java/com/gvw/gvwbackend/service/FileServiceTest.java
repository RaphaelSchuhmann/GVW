package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.Report;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.Score;
import com.gvw.gvwbackend.model.User;
import com.gvw.gvwbackend.util.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  @Mock private LibraryService libraryService;

  @Mock private ReportService reportService;

  @Mock private UserService userService;

  @Mock private TextEditorService editorService;

  @Mock private FileUtils fileUtils;

  @InjectMocks private FileService fileService;

  private Score testScore;
  private Report testReport;
  private User testUser;
  private File testFile;

  @BeforeEach
  void setUp() {
    testFile =
        File.builder()
            .id("file-123")
            .originalName("test.pdf")
            .mimeType("application/pdf")
            .extension("pdf")
            .build();

    testScore = Score.builder().id("score-1").title("Test Score").files(List.of(testFile)).build();

    testReport =
        Report.builder().id("report-1").title("Test Report").attachments(List.of(testFile)).build();

    testUser = User.builder().userId("user-1").role(Role.ADMIN).build();
  }

  @Test
  void streamFilesAsZip_Library_Success() {
    when(libraryService.getScoresDir()).thenReturn("/scores");
    when(libraryService.findScoreById("score-1", ErrorAction.UTILITY)).thenReturn(testScore);

    ResponseEntity<StreamingResponseBody> response =
        fileService.streamFilesAsZip("library", "score-1", "user-1");

    assertNotNull(response);
    assertEquals("application/zip", response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
    assertTrue(
        response
            .getHeaders()
            .get(HttpHeaders.CONTENT_DISPOSITION)
            .get(0)
            .contains("Test Score.zip"));

    StreamingResponseBody body = response.getBody();
    assertNotNull(body);

    assertDoesNotThrow(
        () -> {
          OutputStream out = new ByteArrayOutputStream();
          body.writeTo(out);
        });

    verify(fileUtils).streamFilesAsZip(any(), eq("/scores"), any(), eq(ErrorDomain.FILE_SERVICE));
  }

  @Test
  void streamFilesAsZip_Report_Success() {
    when(editorService.getEditorAssetsDir()).thenReturn("/reports");
    when(reportService.findReportById("report-1", ErrorAction.UTILITY)).thenReturn(testReport);
    when(userService.getUserByUserId("user-1", ErrorAction.UTILITY)).thenReturn(testUser);

    ResponseEntity<StreamingResponseBody> response =
        fileService.streamFilesAsZip("report", "report-1", "user-1");

    assertNotNull(response);
    assertEquals("application/zip", response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));

    StreamingResponseBody body = response.getBody();
    assertNotNull(body);

    assertDoesNotThrow(
        () -> {
          OutputStream out = new ByteArrayOutputStream();
          body.writeTo(out);
        });

    verify(fileUtils).streamFilesAsZip(any(), eq("/reports"), any(), eq(ErrorDomain.FILE_SERVICE));
  }

  @Test
  void streamFilesAsZip_NullService_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> fileService.streamFilesAsZip(null, "score-1", "user-1"));
  }

  @Test
  void streamFilesAsZip_BlankService_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> fileService.streamFilesAsZip("  ", "score-1", "user-1"));
  }

  @Test
  void streamFilesAsZip_NullId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> fileService.streamFilesAsZip("library", null, "user-1"));
  }

  @Test
  void streamFilesAsZip_BlankId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> fileService.streamFilesAsZip("library", "  ", "user-1"));
  }

  @Test
  void streamFilesAsZip_NullUserId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> fileService.streamFilesAsZip("library", "score-1", null));
  }

  @Test
  void streamFilesAsZip_BlankUserId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> fileService.streamFilesAsZip("library", "score-1", "  "));
  }

  @Test
  void streamFilesAsZip_InvalidService_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.streamFilesAsZip("invalid", "score-1", "user-1"));
  }

  @Test
  void streamFilesAsZip_Report_Unauthorized_ThrowsBadRequest() {
    User unauthorizedUser = User.builder().userId("user-1").role(Role.MEMBER).build();

    when(editorService.getEditorAssetsDir()).thenReturn("/reports");
    when(userService.getUserByUserId("user-1", ErrorAction.UTILITY)).thenReturn(unauthorizedUser);

    assertThrows(
        BadRequestException.class,
        () -> fileService.streamFilesAsZip("report", "report-1", "user-1"));
  }

  @Test
  void loadFile_Library_Success() {
    when(libraryService.getScoresDir()).thenReturn("/scores");
    when(libraryService.findScoreById("score-1", ErrorAction.UTILITY)).thenReturn(testScore);

    ResponseEntity<StreamingResponseBody> response =
        fileService.loadFile("library", "score-1", "file-123", "user-1");

    assertNotNull(response);
    assertEquals("application/pdf", response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
    assertTrue(
        response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0).contains("test.pdf"));

    StreamingResponseBody body = response.getBody();
    assertNotNull(body);

    assertDoesNotThrow(
        () -> {
          OutputStream out = new ByteArrayOutputStream();
          body.writeTo(out);
        });

    verify(fileUtils).streamFile(any(), eq("/scores"), any(), eq(ErrorDomain.FILE_SERVICE));
  }

  @Test
  void loadFile_Report_Success() {
    when(editorService.getEditorAssetsDir()).thenReturn("/reports");
    when(reportService.findReportById("report-1", ErrorAction.UTILITY)).thenReturn(testReport);
    when(userService.getUserByUserId("user-1", ErrorAction.UTILITY)).thenReturn(testUser);

    ResponseEntity<StreamingResponseBody> response =
        fileService.loadFile("report", "report-1", "file-123", "user-1");

    assertNotNull(response);
    assertEquals("application/pdf", response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));

    StreamingResponseBody body = response.getBody();
    assertNotNull(body);

    assertDoesNotThrow(
        () -> {
          OutputStream out = new ByteArrayOutputStream();
          body.writeTo(out);
        });

    verify(fileUtils).streamFile(any(), eq("/reports"), any(), eq(ErrorDomain.FILE_SERVICE));
  }

  @Test
  void loadFile_NullMimeType_UsesDefault() {
    File fileWithoutMimeType =
        File.builder()
            .id("file-123")
            .originalName("test.pdf")
            .mimeType(null)
            .extension("pdf")
            .build();

    Score scoreWithoutMimeType =
        Score.builder()
            .id("score-1")
            .title("Test Score")
            .files(List.of(fileWithoutMimeType))
            .build();

    when(libraryService.getScoresDir()).thenReturn("/scores");
    when(libraryService.findScoreById("score-1", ErrorAction.UTILITY))
        .thenReturn(scoreWithoutMimeType);

    ResponseEntity<StreamingResponseBody> response =
        fileService.loadFile("library", "score-1", "file-123", "user-1");

    assertNotNull(response);
    assertEquals(
        "application/octet-stream", response.getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
  }

  @Test
  void loadFile_NullService_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile(null, "score-1", "file-123", "user-1"));
  }

  @Test
  void loadFile_BlankService_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("  ", "score-1", "file-123", "user-1"));
  }

  @Test
  void loadFile_NullDocId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", null, "file-123", "user-1"));
  }

  @Test
  void loadFile_BlankDocId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", "  ", "file-123", "user-1"));
  }

  @Test
  void loadFile_NullFileId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", "score-1", null, "user-1"));
  }

  @Test
  void loadFile_BlankFileId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", "score-1", "  ", "user-1"));
  }

  @Test
  void loadFile_NullUserId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", "score-1", "file-123", null));
  }

  @Test
  void loadFile_BlankUserId_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", "score-1", "file-123", "  "));
  }

  @Test
  void loadFile_InvalidService_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("invalid", "score-1", "file-123", "user-1"));
  }

  @Test
  void loadFile_Library_FileNotFound_ThrowsBadRequest() {
    when(libraryService.getScoresDir()).thenReturn("/scores");
    when(libraryService.findScoreById("score-1", ErrorAction.UTILITY)).thenReturn(testScore);

    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("library", "score-1", "nonexistent", "user-1"));
  }

  @Test
  void loadFile_Report_Unauthorized_ThrowsBadRequest() {
    User unauthorizedUser = User.builder().userId("user-1").role(Role.MEMBER).build();

    when(editorService.getEditorAssetsDir()).thenReturn("/reports");
    when(userService.getUserByUserId("user-1", ErrorAction.UTILITY)).thenReturn(unauthorizedUser);

    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("report", "report-1", "file-123", "user-1"));
  }

  @Test
  void loadFile_Report_FileNotFound_ThrowsBadRequest() {
    when(editorService.getEditorAssetsDir()).thenReturn("/reports");
    when(reportService.findReportById("report-1", ErrorAction.UTILITY)).thenReturn(testReport);
    when(userService.getUserByUserId("user-1", ErrorAction.UTILITY)).thenReturn(testUser);

    assertThrows(
        BadRequestException.class,
        () -> fileService.loadFile("report", "report-1", "nonexistent", "user-1"));
  }

  @Test
  void streamFilesAsZip_SanitizesTitle() {
    Score scoreWithSpecialChars =
        Score.builder().id("score-1").title("Test\"Score\r\n").files(List.of(testFile)).build();

    when(libraryService.getScoresDir()).thenReturn("/scores");
    when(libraryService.findScoreById("score-1", ErrorAction.UTILITY))
        .thenReturn(scoreWithSpecialChars);

    ResponseEntity<StreamingResponseBody> response =
        fileService.streamFilesAsZip("library", "score-1", "user-1");

    assertNotNull(response);
    String contentDisposition = response.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION).get(0);
    System.out.println(contentDisposition);
    assertTrue(contentDisposition.contains("Test_Score_.zip"));
    assertFalse(contentDisposition.contains("\r"));
    assertFalse(contentDisposition.contains("\n"));
  }
}
