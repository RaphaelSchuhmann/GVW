package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.model.*;
import com.gvw.gvwbackend.util.FileUtils;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Record holding internal routing payload data for archiving multiple files into a ZIP stream.
 *
 * @param directory the base storage directory path on disk for the requested service domain
 * @param data a pair containing the document title as key and the list of associated files as value
 */
record ZipPayload(String directory, Pair<String, List<File>> data) {}

/**
 * Record holding internal routing payload data for streaming a single file.
 *
 * @param directory the base storage directory path on disk for the requested service domain
 * @param file the resolved target file metadata object
 */
record FilePayload(String directory, File file) {}

/**
 * Service responsible for processing, authorization checking, and streaming file resources.
 *
 * <p>Handles streaming both single raw files and multi-file ZIP archives across different domains
 * (e.g., sheet music library files, report attachment assets) using asynchronous streaming response
 * bodies.
 */
@Service
public class FileService {
  private final LibraryService libraryService;
  private final ReportService reportService;
  private final UserService userService;
  private final TextEditorService editorService;
  private final FileUtils fileUtils;

  /**
   * Constructs a new {@code FileService} with required service and utility dependencies.
   *
   * @param libraryService service for managing score library documents
   * @param reportService service for managing report documents and attachments
   * @param userService service for retrieving user records and verifying access roles
   * @param editorService service for text editor assets and configuration
   * @param fileUtils utility component providing direct file system streaming routines
   */
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

  /**
   * Bundles all files belonging to a specific document into a ZIP archive and streams the output
   * directly to the client.
   *
   * @param service the target sub-service identifier (e.g., {@code "library"} or {@code "report"})
   * @param id the unique document identifier containing the files
   * @param userId the unique identifier of the requesting user
   * @return a {@link ResponseEntity} wrapping a {@link StreamingResponseBody} with content type
   *     {@code application/zip}
   * @throws BadRequestException if any input parameter is missing or blank, or if an unsupported
   *     service name is provided
   */
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
          case "library" -> new ZipPayload(libraryService.getScoresDir(), getScorePair(id));
          case "report" ->
              new ZipPayload(editorService.getEditorAssetsDir(), getReportPair(id, userId));
          default ->
              throw new BadRequestException(
                  String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 400)));
        };

    Pair<String, List<File>> item = payload.data();
    String rawTitle = item.first() != null ? item.first() : "download";

    String sanitizedTitle =
        rawTitle.replaceAll("[^a-zA-Z0-9 äöüÄÖÜß\\-_.]", "_").trim().replaceAll("_+", "_");

    if (sanitizedTitle.isBlank()) {
      sanitizedTitle = "download";
    }

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/zip")
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedTitle + ".zip\"")
        .body(
            out ->
                fileUtils.streamFilesAsZip(
                    item.second(), payload.directory(), out, ErrorDomain.FILE_SERVICE));
  }

  /**
   * Retrieves metadata for a single file and streams its raw content inline to the caller.
   *
   * @param service the target sub-service identifier (e.g., {@code "library"} or {@code "report"})
   * @param docId the unique identifier of the parent document
   * @param fileId the unique identifier of the target file to stream
   * @param userId the unique identifier of the requesting user
   * @return a {@link ResponseEntity} wrapping a {@link StreamingResponseBody} configured for inline
   *     file viewing
   * @throws BadRequestException if any required argument is missing/blank, if an invalid service is
   *     passed, or if the file cannot be located
   */
  public ResponseEntity<StreamingResponseBody> loadFile(
      String service, String docId, String fileId, String userId) {
    if (service == null
        || service.isBlank()
        || docId == null
        || docId.isBlank()
        || fileId == null
        || fileId.isBlank()
        || userId == null
        || userId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 400)));
    }

    FilePayload payload =
        switch (service) {
          case "library" ->
              new FilePayload(libraryService.getScoresDir(), getScoreFile(docId, fileId));
          case "report" ->
              new FilePayload(
                  editorService.getEditorAssetsDir(), getReportFile(docId, fileId, userId));
          default ->
              throw new BadRequestException(
                  String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 400)));
        };

    File file = payload.file();

    String mimeType =
        file.getMimeType() != null && !file.getMimeType().isBlank()
            ? file.getMimeType()
            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, mimeType)
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getOriginalName() + "\"")
        .body(
            out -> fileUtils.streamFile(file, payload.directory(), out, ErrorDomain.FILE_SERVICE));
  }

  /**
   * Fetches score metadata from the library and extracts its title and file attachment list.
   *
   * @param id the unique score document identifier
   * @return a {@link Pair} containing the score title as key and list of associated files as value
   */
  private Pair<String, List<File>> getScorePair(String id) {
    Score score = libraryService.findScoreById(id, ErrorAction.UTILITY);
    return new Pair<>(score.getTitle(), score.getFiles());
  }

  /**
   * Verifies access privileges and retrieves report metadata along with its attachment list.
   *
   * @param id the unique report document identifier
   * @param userId the unique identifier of the user performing the request
   * @return a {@link Pair} containing the report title as key and list of attachment files as value
   * @throws BadRequestException if the user lacks access rights or the report is not found
   */
  private Pair<String, List<File>> getReportPair(String id, String userId) {
    hasFileAccess(userId, Set.of(Role.ADMIN, Role.BOARD_MEMBER, Role.SECRETARY));

    Report report = reportService.findReportById(id, ErrorAction.UTILITY);
    return new Pair<>(report.getTitle(), report.getAttachments());
  }

  /**
   * Finds a specific score file metadata entry by its unique identifier.
   *
   * @param id the parent score document identifier
   * @param fileId the target file identifier
   * @return the resolved {@link File} metadata object
   * @throws BadRequestException if the score or specific file entry is not found (404 error code)
   */
  private File getScoreFile(String id, String fileId) {
    Score score = libraryService.findScoreById(id, ErrorAction.UTILITY);

    return score.getFiles().stream()
        .filter(file -> file.getId().equals(fileId))
        .findFirst()
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 404))));
  }

  /**
   * Validates user authorization and retrieves a specific report attachment file metadata entry.
   *
   * @param id the parent report document identifier
   * @param fileId the target file identifier
   * @param userId the requesting user's unique identifier
   * @return the resolved {@link File} metadata object
   * @throws BadRequestException if authorized checks fail (403) or the file entry is not found
   *     (404)
   */
  private File getReportFile(String id, String fileId, String userId) {
    hasFileAccess(userId, Set.of(Role.ADMIN, Role.BOARD_MEMBER, Role.SECRETARY));

    Report report = reportService.findReportById(id, ErrorAction.UTILITY);

    return report.getAttachments().stream()
        .filter(file -> file.getId().equals(fileId))
        .findFirst()
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 404))));
  }

  /**
   * Validates whether the user represented by the given ID holds one of the allowed role
   * privileges.
   *
   * @param userId the unique identifier of the target user
   * @param validRoles a set of authorized {@link Role} values permitted for the operation
   * @throws BadRequestException if the user's role is not present within the permitted role set
   *     (403 error code)
   */
  private void hasFileAccess(String userId, Set<Role> validRoles) {
    User user = userService.getUserByUserId(userId, ErrorAction.UTILITY);
    Role role = user.getRole();

    if (!validRoles.contains(role)) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.FILE_SERVICE.createCode(ErrorAction.UTILITY, 403)));
    }
  }
}
