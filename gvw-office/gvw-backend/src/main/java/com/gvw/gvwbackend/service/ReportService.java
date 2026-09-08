package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddReportRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateDocumentAttachmentsDTO;
import com.gvw.gvwbackend.dto.request.UpdateReportDescriptionRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateReportRequestDTO;
import com.gvw.gvwbackend.dto.response.*;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.*;
import com.gvw.gvwbackend.util.FileUtils;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service responsible for managing reports.
 *
 * <p>Provides functionality for creating, retrieving, updating, deleting, and searching reports.
 * Handles persistence through {@link DbService}, editor asset management through {@link
 * TextEditorService}, and real-time client updates through {@link SseService}.
 *
 * <p>Report updates also manage linked editor assets and attachments, including cleanup of unused
 * files.
 */
@Service
public class ReportService {
  private final DbService dbService;
  private final SseService sseService;
  private final TextEditorService editorService;
  private final FileUtils fileUtils;
  private static final Logger log = LoggerFactory.getLogger(ReportService.class);

  /**
   * Creates a new report service instance.
   *
   * @param dbService service used for report persistence
   * @param sseService service used for broadcasting report changes
   * @param editorService service used for managing editor content and assets
   */
  public ReportService(
      DbService dbService,
      SseService sseService,
      TextEditorService editorService,
      FileUtils fileUtils) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.editorService = editorService;
    this.fileUtils = fileUtils;
  }

  /**
   * Retrieves all available reports.
   *
   * <p>Loads reports from storage and converts them into lightweight response objects suitable for
   * report listings.
   *
   * @return list of available reports
   */
  public List<ReportResponseDTO> getReports() {
    List<Report> reports = dbService.findAll("reports", Report.class);

    if (reports.isEmpty()) {
      return List.of();
    }

    return reports.stream()
        .map(
            m ->
                new ReportResponseDTO(
                    m.getId(),
                    m.getTitle(),
                    m.getAuthor(),
                    m.getType(),
                    m.getDescription(),
                    m.getCreatedAt()))
        .toList();
  }

  /**
   * Checks whether a report exists.
   *
   * @param id identifier of the report
   * @throws BadRequestException if the identifier is empty
   * @throws NotFoundException if no report exists with the given identifier
   */
  public void checkReport(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.CHECK, 400)));
    }

    Report report = dbService.findById("reports", id, Report.class);
    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.CHECK, 404)));
    }
  }

  /**
   * Creates a new report.
   *
   * <p>Initializes the report metadata and creates an empty text editor block as the starting
   * content. After successful creation, a refresh event is broadcast to connected clients.
   *
   * @param request report creation data
   */
  public void createReport(AddReportRequestDTO request) {
    Report report = new Report();
    report.setTitle(request.title());
    report.setAuthor(request.author());
    report.setDescription(request.description());
    report.setType(request.type());
    report.setCreatedAt(Instant.now().toString());
    report.setLastEditedBy(request.author());

    TextEditorBlock startBlock = new TextEditorBlock();
    startBlock.setId(UUID.randomUUID().toString());
    startBlock.setType(TextEditorBlockType.TEXT);
    startBlock.setData("");
    report.setContents(List.of(startBlock));

    dbService.insert("reports", report);

    sseService.sendRefresh("REPORTS");
  }

  /**
   * Retrieves a complete report including editor content and metadata.
   *
   * <p>Additionally calculates plain text statistics such as word count and estimated reading time.
   *
   * @param id identifier of the report
   * @return complete report information
   * @throws BadRequestException if the identifier is invalid
   * @throws NotFoundException if the report does not exist
   */
  public FullReportResponseDTO getReport(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.READ_ONE, 400)));
    }

    Report report = dbService.findById("reports", id, Report.class);

    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.READ_ONE, 404)));
    }

    String plainText = editorService.convertBlocksToPlainText(report.getContents());

    List<String> words =
        Arrays.stream(plainText.split("\\s+")).filter(word -> !word.isEmpty()).toList();

    List<String> filenames = new ArrayList<>();

    if (report.getAttachments() != null) {
      for (File file : report.getAttachments()) {
        filenames.add(file.getOriginalName());
      }
    }

    return new FullReportResponseDTO(
        report.getId(),
        report.getTitle(),
        report.getAuthor(),
        report.getRev(),
        report.getDescription(),
        editorService.getReadingTime(report.getContents()),
        words.size(),
        report.getCreatedAt(),
        report.getLastEditedBy(),
        report.getType(),
        report.getContents(),
        filenames);
  }

  /**
   * Verifies that an editor asset belongs to a report.
   *
   * <p>Used to prevent unauthorized access to files that are not referenced by the requested report
   * content.
   *
   * @param documentId identifier of the report
   * @param filename asset filename to verify
   * @throws BadRequestException if parameters are invalid or the asset is not referenced by the
   *     report
   * @throws NotFoundException if the report does not exist
   */
  public void verifyAssetOwnership(String documentId, String filename) {
    if (documentId == null || documentId.isBlank() || filename == null || filename.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 400)));
    }

    Report report = dbService.findById("reports", documentId, Report.class);
    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 404)));
    }

    Set<String> linkedFileIds = editorService.extractFileIds(report.getContents());

    if (!linkedFileIds.contains(filename)) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 400)));
    }
  }

  /**
   * Deletes a report and all associated assets.
   *
   * <p>Removes the report from storage, deletes editor block assets, removes uploaded attachments
   * from disk, and broadcasts a report refresh event.
   *
   * @param id identifier of the report
   * @throws BadRequestException if the identifier is invalid
   * @throws NotFoundException if the report does not exist
   */
  public void deleteReport(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.DELETE, 400)));
    }

    Report report = dbService.findById("reports", id, Report.class);
    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.DELETE, 404)));
    }

    dbService.delete("reports", report.getId(), report.getRev());

    log.debug("Purging report editor assets");
    editorService.purgeAllBlockAssets(report.getContents());

    List<File> attachments = report.getAttachments();
    if (!attachments.isEmpty()) {
      for (File file : attachments) {
        try {
          fileUtils.deleteFile(
              file.getId() + "." + file.getExtension(), editorService.getEditorAssetsDir());
        } catch (Exception ex) {
          log.error(
              "Failed to purge unlinked attachment asset from file system: {}",
              file.getId() + "." + file.getExtension(),
              ex);
        }
      }
    }

    sseService.sendRefresh("REPORTS");
  }

  /**
   * Searches through report contents for a given search term.
   *
   * <p>Uses deep text search on editor content and returns matching reports together with
   * surrounding text snippets.
   *
   * @param input search term
   * @return matching reports with search context
   */
  public List<ReportSearchResponseDTO> reportDeepSearch(String input) {
    if (input == null || input.isBlank()) {
      return List.of();
    }

    List<Report> reports = dbService.findAll("reports", Report.class);

    if (reports.isEmpty()) {
      return List.of();
    }

    List<TextDocumentSearchResult<Report>> results = editorService.deepSearch(reports, input);

    return results.stream()
        .map(
            m ->
                new ReportSearchResponseDTO(
                    m.getDocument().getId(),
                    m.getDocument().getTitle(),
                    m.getDocument().getAuthor(),
                    m.getDocument().getType(),
                    m.getSnippet(),
                    m.getDocument().getCreatedAt()))
        .toList();
  }

  /**
   * Updates report content and handles editor asset synchronization.
   *
   * <p>Processes uploaded editor files, replaces temporary image references, updates the report
   * content, removes unused assets, and broadcasts a refresh event after successful persistence.
   *
   * @param request updated report data
   * @param files newly uploaded editor files
   * @return new database revision of the updated report
   * @throws BadRequestException if uploaded files or content are invalid
   * @throws NotFoundException if the report does not exist
   */
  public String updateReport(UpdateReportRequestDTO request, List<MultipartFile> files) {
    Report report = dbService.findById("reports", request.id(), Report.class);
    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 404)));
    }

    log.debug("Starting report update");

    List<TextEditorBlock> oldContents = report.getContents();
    Map<String, String> newlyUploadedFiles = new HashMap<>();

    try {
      if (files != null && !files.isEmpty()) {
        log.debug("Processing {} uploaded editor files", files.size());
        newlyUploadedFiles = editorService.processUploadedFiles(files, ErrorAction.UPDATE);
        log.debug("Editor files processed successfully");
      }

      // Update Image block data to permanent internal filenames
      List<TextEditorBlock> blocks = request.content();
      for (TextEditorBlock block : blocks) {
        if (block.getType() != TextEditorBlockType.IMAGE) continue;

        String tempId = block.getData();
        if (tempId.startsWith("temp_")) {
          String realId = newlyUploadedFiles.get(tempId);
          if (realId != null) {
            block.setData(realId);
          } else {
            log.error("Missing file for temp ID: {}", tempId);
            throw new BadRequestException(
                String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 400)));
          }
        }
      }

      report.setTitle(request.title());
      report.setLastEditedBy(request.editor());
      report.setContents(request.content());
      report.setRev(request.rev());

      String rev = dbService.update("reports", report.getId(), report);

      log.debug("Synchronizing report editor assets");
      editorService.synchronizeBlockAssets(oldContents, request.content(), ErrorAction.UPDATE);
      log.debug("Report editor assets synchronized successfully");

      sseService.sendRefresh("REPORTS");

      log.debug("Report update completed successfully");

      return rev;
    } catch (Exception e) {
      log.error("Update failed for report ID: {}", request.id(), e);

      if (e instanceof BadRequestException) throw (BadRequestException) e;
      if (e instanceof NotFoundException) throw (NotFoundException) e;

      throw new RuntimeException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 500)), e);
    }
  }

  /**
   * Updates the description of an existing report.
   *
   * <p>If no description is provided, a default placeholder description is stored instead.
   *
   * @param request updated description information
   * @return new database revision of the updated report
   * @throws NotFoundException if the report does not exist
   */
  public String updateReportDescription(UpdateReportDescriptionRequestDTO request) {
    String description = request.description();

    if (description.isBlank()) {
      description = "Keine Beschreibung";
    }

    Report report = dbService.findById("reports", request.id(), Report.class);
    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UPDATE, 404)));
    }

    report.setDescription(description);
    report.setRev(request.rev());

    String rev = dbService.update("reports", report.getId(), report);

    sseService.sendRefresh("REPORTS");

    return rev;
  }

  /**
   * Updates the attachments of a report.
   *
   * <p>Stores newly uploaded files, removes deleted attachments from disk, updates the report
   * metadata, and broadcasts a refresh event.
   *
   * <p>If the update fails, newly stored files are removed to prevent orphaned files on disk.
   *
   * @param request attachment update information
   * @param files newly uploaded files
   * @param reportId identifier of the report
   * @return new database revision of the updated report
   * @throws BadRequestException if the request is invalid or the revision does not match
   * @throws NotFoundException if the report does not exist
   */
  public String updateAttachments(
      UpdateDocumentAttachmentsDTO request, List<MultipartFile> files, String reportId) {
    if (reportId == null || reportId.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 400)));
    }

    Report report = dbService.findById("reports", reportId, Report.class);
    if (report == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 404)));
    }

    if (!report.getRev().equals(request.rev())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 409)));
    }

    List<File> oldAttachments =
        report.getAttachments() != null ? report.getAttachments() : new ArrayList<>();

    List<File> filesToPurgeFromDisk =
        oldAttachments.stream()
            .filter(file -> !request.attachments().contains(file.getOriginalName()))
            .toList();

    List<File> newlyWrittenFilesToDisk = new ArrayList<>();

    try {
      log.debug("Storing uploaded report attachments");
      newlyWrittenFilesToDisk =
          fileUtils.storeFiles(
              files, editorService.getEditorAssetsDir(), ErrorDomain.REPORT, ErrorAction.UPDATE);
      log.debug("Report attachments stored successfully");

      List<File> finalAttachmentList = new ArrayList<>();

      if (request.attachments() != null && !request.attachments().isEmpty()) {
        for (String name : request.attachments()) {
          File matchedFile =
              oldAttachments.stream()
                  .filter(old -> old.getOriginalName().equals(name))
                  .findFirst()
                  .orElse(null);

          if (matchedFile == null) {
            matchedFile =
                newlyWrittenFilesToDisk.stream()
                    .filter(newFile -> newFile.getOriginalName().equals(name))
                    .findFirst()
                    .orElse(null);
          }

          if (matchedFile != null) {
            finalAttachmentList.add(matchedFile);
          }
        }
      }

      for (File newFile : newlyWrittenFilesToDisk) {
        if (!finalAttachmentList.contains(newFile)) {
          finalAttachmentList.add(newFile);
        }
      }

      report.setAttachments(finalAttachmentList);
      report.setRev(request.rev());

      String rev = dbService.update("reports", report.getId(), report);

      for (File deadFile : filesToPurgeFromDisk) {
        try {
          fileUtils.deleteFile(
              deadFile.getId() + "." + deadFile.getExtension(), editorService.getEditorAssetsDir());
        } catch (Exception ex) {
          log.error(
              "Failed to purge unlinked attachment asset from file system: {}",
              deadFile.getId() + "." + deadFile.getExtension(),
              ex);
        }
      }

      sseService.sendRefresh("REPORTS");

      return rev;
    } catch (Exception e) {
      log.error(
          "Attachment update transaction failed for report ID: {}. Triggering system rollback",
          reportId,
          e);

      for (File failedFile : newlyWrittenFilesToDisk) {
        try {
          fileUtils.deleteFile(
              failedFile.getId() + "." + failedFile.getExtension(),
              editorService.getEditorAssetsDir());
        } catch (Exception rollbackEx) {
          log.error(
              "Critical: Failed to remove orphaned file during transaction rollback: {}",
              failedFile.getId() + "." + failedFile.getExtension(),
              rollbackEx);
        }
      }

      if (e instanceof BadRequestException) throw (BadRequestException) e;
      throw new RuntimeException(
          String.valueOf(ErrorDomain.REPORT.createCode(ErrorAction.UPDATE, 500)), e);
    }
  }

  /**
   * Streams report attachments as a ZIP archive.
   *
   * <p>Creates a ZIP archive directly on the provided output stream without loading all files into
   * memory.
   *
   * @param files files to include in the archive
   * @param out output stream receiving the ZIP data
   * @throws RuntimeException if archive creation fails
   */
  public void streamFilesAsZip(List<File> files, OutputStream out) {
    fileUtils.streamFilesAsZip(files, editorService.getEditorAssetsDir(), out, ErrorDomain.REPORT);
  }
}
