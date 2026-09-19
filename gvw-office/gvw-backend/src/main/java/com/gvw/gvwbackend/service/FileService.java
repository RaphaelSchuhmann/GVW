package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.model.*;
import com.gvw.gvwbackend.util.FileUtils;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

record ZipPayload(String directory, Pair<String, List<File>> data) {}

@Service
public class FileService {
  private final LibraryService libraryService;
  private final ReportService reportService;
  private final UserService userService;
  private final TextEditorService editorService;
  private final FileUtils fileUtils;

  public FileService(
      LibraryService libraryService,
      ReportService reportService,
      UserService userService,
      TextEditorService editorService,
      FileUtils fileUtils) {
    this.libraryService = libraryService;
    this.reportService = reportService;
    this.userService = userService;
    this.editorService = editorService;
    this.fileUtils = fileUtils;
  }

  public ResponseEntity<StreamingResponseBody> streamFilesAsZip(
      String service, String id, String userId) {
    if (service == null
        || service.isBlank()
        || id == null
        || id.isBlank()
        || userId == null
        || userId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 400)));
    }

    ZipPayload payload =
        switch (service) {
          case "library" -> new ZipPayload(libraryService.getScoresDir(), getScoreData(id));
          case "report" ->
              new ZipPayload(editorService.getEditorAssetsDir(), getReportData(id, userId));
          default ->
              throw new BadRequestException(
                  String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 400)));
        };

    Pair<String, List<File>> item = payload.data();
    String sanitizedTitle = item.first().replaceAll("[\"\r\n]", "_");

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/zip")
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedTitle + ".zip\"")
        .body(
            out ->
                fileUtils.streamFilesAsZip(
                    item.second(), payload.directory(), out, ErrorDomain.FILE_SERVICE));
  }

  private Pair<String, List<File>> getScoreData(String id) {
    Score score = libraryService.findScoreById(id, ErrorAction.UTILITY);
    return new Pair<>(score.getTitle(), score.getFiles());
  }

  private Pair<String, List<File>> getReportData(String id, String userId) {
    User user = userService.getUserByUserId(userId, ErrorAction.UTILITY);
    Role role = user.getRole();

    if (!Set.of(Role.ADMIN, Role.BOARD_MEMBER, Role.SECRETARY).contains(role)) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 403)));
    }

    Report report = reportService.findReportById(id, ErrorAction.UTILITY);
    return new Pair<>(report.getTitle(), report.getAttachments());
  }
}
