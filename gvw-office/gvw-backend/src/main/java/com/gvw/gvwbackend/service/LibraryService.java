package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddScoreRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateScoreRequestDTO;
import com.gvw.gvwbackend.dto.response.FullScoreResponseDTO;
import com.gvw.gvwbackend.dto.response.ScoreResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.Score;
import com.gvw.gvwbackend.util.FileUtils;
import java.io.OutputStream;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service responsible for managing the score library.
 *
 * <p>Handles score CRUD operations, file storage, ZIP streaming, duplicate detection, and
 * synchronization notifications via SSE.
 */
@Service
public class LibraryService {
  private static final Logger log = LoggerFactory.getLogger(LibraryService.class);
  private final DbService dbService;
  private final SseService sseService;
  private final FileUtils fileUtils;

  @Value("${scores.directory:./api-data/scores}")
  private String scoresDir;

  public LibraryService(DbService dbService, SseService sseService, FileUtils fileUtils) {
    this.dbService = dbService;
    this.sseService = sseService;
    this.fileUtils = fileUtils;
  }

  /**
   * Retrieves all scores stored in the library.
   *
   * <p>Loads score documents from the database and converts them into response DTOs containing
   * metadata and attachment names.
   *
   * @return response object containing all available scores
   */
  public List<ScoreResponseDTO> getAllScores() {
    List<Score> scores = dbService.findAll("library", Score.class);

    if (scores.isEmpty()) {
      return List.of();
    }

    return scores.stream()
        .map(
            m ->
                new ScoreResponseDTO(
                    m.getId(),
                    m.getScoreId(),
                    m.getTitle(),
                    m.getArtist(),
                    m.getType(),
                    m.getVoices(),
                    m.getVoiceCount()))
        .toList();
  }

  /**
   * Retrieves a complete score.
   *
   * @param id identifier of the sore
   * @return complete score information
   * @throws BadRequestException if the identifier is invalid
   * @throws NotFoundException if the score does not exist
   */
  public FullScoreResponseDTO getFullScore(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.READ_ONE, 400)));
    }

    Score score = dbService.findById("library", id, Score.class);
    if (score == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.READ_ONE, 404)));
    }

    return new FullScoreResponseDTO(
        score.getId(),
        score.getRev(),
        score.getScoreId(),
        score.getTitle(),
        score.getArtist(),
        score.getType(),
        score.getVoices(),
        score.getVoiceCount(),
        score.getFiles() != null
            ? score.getFiles().stream().map(File::getOriginalName).toList()
            : List.of());
  }

  /**
   * Checks whether a score exists.
   *
   * <p>This method is intended for validation before performing operations that require an existing
   * score document.
   *
   * @param id identifier of the score to check
   * @throws BadRequestException if the identifier is empty or null
   * @throws NotFoundException if no score exists with the given identifier
   */
  public void checkScore(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CHECK, 400)));
    }

    Score score = dbService.findById("library", id, Score.class);
    if (score == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CHECK, 404)));
    }
  }

  /**
   * Creates a new score entry and stores all uploaded files.
   *
   * <p>The operation stores uploaded files first, creates the database entry, and broadcasts a
   * library refresh event after successful creation.
   *
   * <p>If creation fails after files have been stored, all created files are removed to prevent
   * orphaned assets.
   *
   * @param request metadata of the score to create
   * @param files uploaded score files
   * @throws ConflictException if a score with identical metadata already exists
   * @throws RuntimeException if creation fails internally
   */
  public void createScore(AddScoreRequestDTO request, List<MultipartFile> files) {
    if (existsInLibrary(request.scoreId(), request.title(), request.artist())) {
      throw new ConflictException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CREATE, 409)));
    }

    List<File> metaList = new ArrayList<>();
    try {
      log.debug("Storing score files");
      metaList = fileUtils.storeFiles(files, scoresDir, ErrorDomain.LIBRARY, ErrorAction.CREATE);
      log.debug("Score files stored successfully");

      Score score =
          Score.builder()
              .scoreId(request.scoreId())
              .title(request.title())
              .artist(request.artist())
              .type(request.type())
              .voices(request.voices())
              .voiceCount(request.voiceCount())
              .files(metaList)
              .build();

      log.debug("Inserting new score into database");
      dbService.insert("library", score);
      log.debug("Score inserted successfully");

      try {
        sseService.broadcastRefresh("SCORES");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast SCORES refresh", ex);
      }
    } catch (Exception e) {
      for (File orphan : metaList) {
        fileUtils.deleteFile(orphan.getId() + "." + orphan.getExtension(), scoresDir);
      }

      if (e instanceof ConflictException)
        throw new ConflictException(
            String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CREATE, 409)));
      throw new RuntimeException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CREATE, 500)), e);
    }
  }

  /**
   * Deletes a score and all associated files.
   *
   * <p>Removes the database entry first and then deletes all physical files belonging to the score.
   *
   * @param id identifier of the score to delete
   * @throws BadRequestException if the identifier is invalid
   * @throws NotFoundException if the score does not exist
   */
  public void deleteScore(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.DELETE, 400)));
    }

    Score score = dbService.findById("library", id, Score.class);
    if (score == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.DELETE, 404)));
    }

    if (score.getFiles() != null) {
      for (File file : score.getFiles()) {
        fileUtils.deleteFile(file.getId() + "." + file.getExtension(), scoresDir);
      }
    }

    log.debug("Deleting score from database");
    dbService.delete("library", score.getId(), score.getRev());
    log.debug("Score deleted successfully");

    try {
      sseService.broadcastRefresh("SCORES");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast SCORES refresh", ex);
    }
  }

  /**
   * Streams multiple score files as a ZIP archive.
   *
   * <p>Only files existing on disk are included. Missing files are skipped and logged.
   *
   * @param files metadata of files that should be included
   * @param out output stream receiving the generated ZIP archive
   * @throws RuntimeException if ZIP creation fails
   */
  public void streamFilesAsZip(List<File> files, OutputStream out) {
    fileUtils.streamFilesAsZip(files, scoresDir, out, ErrorDomain.LIBRARY);
  }

  /**
   * Updates an existing score entry.
   *
   * <p>Supports metadata changes, file additions, and file removals. Newly uploaded files are
   * rolled back if the database update fails.
   *
   * @param request updated score information
   * @param newFiles files to add to the score
   * @param requestRemovedFiles names of files to remove
   * @return new database revision identifier
   * @throws NotFoundException if the score does not exist
   * @throws RuntimeException if updating fails
   */
  public String updateScore(
      UpdateScoreRequestDTO request,
      List<MultipartFile> newFiles,
      List<String> requestRemovedFiles) {
    Score score = dbService.findById("library", request.id(), Score.class);
    if (score == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UPDATE, 404)));
    }

    List<File> newlyStoredFiles = new ArrayList<>();
    List<File> filesToPhysicallyDelete = new ArrayList<>();

    List<File> updatedFileList =
        new ArrayList<>(score.getFiles() != null ? score.getFiles() : List.of());

    try {
      if (requestRemovedFiles != null && !requestRemovedFiles.isEmpty()) {
        Iterator<File> iterator = updatedFileList.iterator();
        while (iterator.hasNext()) {
          File file = iterator.next();
          if (requestRemovedFiles.contains(file.getOriginalName())) {
            filesToPhysicallyDelete.add(file);
            iterator.remove();
          }
        }
      }

      if (newFiles != null && !newFiles.isEmpty()) {
        log.debug("Storing new score files");
        newlyStoredFiles =
            fileUtils.storeFiles(newFiles, scoresDir, ErrorDomain.LIBRARY, ErrorAction.UPDATE);
        log.debug("New score files stored successfully");

        updatedFileList.addAll(newlyStoredFiles);
      }

      score.setRev(request.rev());
      score.setFiles(updatedFileList);
      score.setScoreId(request.scoreId());
      score.setTitle(request.title());
      score.setArtist(request.artist());
      score.setType(request.type());
      score.setVoices(request.voices());
      score.setVoiceCount(request.voiceCount());

      log.debug("Updating score in database");
      Map<String, Object> resp = dbService.update("library", score.getId(), score);
      log.debug("Score updated successfully");

      if (resp == null || !resp.containsKey("rev")) {
        throw new RuntimeException(
            String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UPDATE, 500)));
      }

      for (File oldFile : filesToPhysicallyDelete) {
        fileUtils.deleteFile(oldFile.getId() + "." + oldFile.getExtension(), scoresDir);
      }

      try {
        sseService.broadcastRefresh("SCORES");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast SCORES refresh", ex);
      }

      return (String) resp.get("rev");
    } catch (Exception e) {
      log.error("Update failed. Rolling back new uploads.", e);
      for (File newFile : newlyStoredFiles) {
        fileUtils.deleteFile(newFile.getId() + "." + newFile.getExtension(), scoresDir);
      }

      throw new RuntimeException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UPDATE, 500)), e);
    }
  }

  /**
   * Checks whether a score with the given identifying metadata already exists.
   *
   * @param scoreId external score identifier
   * @param title score title
   * @param artist score artist
   * @return true if an identical score already exists
   */
  private boolean existsInLibrary(String scoreId, String title, String artist) {
    Map<String, Object> query =
        Map.of("selector", Map.of("scoreId", scoreId, "title", title, "artist", artist));
    List<Score> result = dbService.findByQuery("library", query, Score.class);

    return result != null && !result.isEmpty();
  }
}
