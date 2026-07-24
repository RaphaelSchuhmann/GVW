package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddScoreRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateScoreRequestDTO;
import com.gvw.gvwbackend.dto.response.ScoreResponseDTO;
import com.gvw.gvwbackend.dto.response.ScoresResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.Score;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
public class LibraryService {
  private final DbService dbService;
  private final ObjectMapper mapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(LibraryService.class);
  private final SseService sseService;
  private static final long MAX_FILE_SIZE = 8 * 1024 * 1024;

  @Value("${scores.directory:./api-data/scores}")
  private String scoresDir;

  public LibraryService(DbService dbService, SseService sseService) {
    this.dbService = dbService;
    this.sseService = sseService;
  }

  public ScoresResponseDTO getAllScores() {
    List<Map<String, Object>> scoresRaw = dbService.findAll("library");

    List<Score> scores =
        scoresRaw.stream().map(map -> mapper.convertValue(map, Score.class)).toList();

    if (scores.isEmpty()) {
      return new ScoresResponseDTO(List.of());
    }

    List<ScoreResponseDTO> responseScores =
        scores.stream()
            .map(
                m ->
                    new ScoreResponseDTO(
                        m.getId(),
                        m.getRev(),
                        m.getScoreId(),
                        m.getTitle(),
                        m.getArtist(),
                        m.getType(),
                        m.getVoices(),
                        m.getVoiceCount(),
                        m.getFiles() != null
                            ? m.getFiles().stream().map(File::getOriginalName).toList()
                            : List.of()))
            .toList();

    return new ScoresResponseDTO(responseScores);
  }

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

  public void createScore(AddScoreRequestDTO request, List<MultipartFile> files) {
    if (existsInLibrary(request.scoreId(), request.title(), request.artist())) {
      throw new ConflictException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CREATE, 409)));
    }

    List<File> metaList = new ArrayList<>();
    try {
      metaList = storeFiles(files, ErrorAction.CREATE);

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

      dbService.insert("library", score);

      try {
        sseService.broadcastRefresh("SCORES");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast SCORES refresh: ", ex);
      }
    } catch (Exception e) {
      for (File orphan : metaList) {
        deleteFile(orphan.getId() + "." + orphan.getExtension(), ErrorAction.CREATE);
      }

      if (e instanceof ConflictException)
        throw new ConflictException(
            String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CREATE, 409)));
      throw new RuntimeException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.CREATE, 500)), e);
    }
  }

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
        deleteFile(file.getId() + "." + file.getExtension(), ErrorAction.DELETE);
      }
    }

    dbService.delete("library", score.getId(), score.getRev());

    try {
      sseService.broadcastRefresh("SCORES");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast SCORES refresh: ", ex);
    }
  }

  public void streamFilesAsZip(List<File> files, OutputStream out) {
    Path root = Paths.get(scoresDir);

    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (File file : files) {
        Path filePath = root.resolve(file.getId() + "." + file.getExtension());

        if (Files.exists(filePath)) {
          String entryName =
              file.getOriginalName()
                  .replaceAll("[\r\n]", "_")
                  .replaceAll("\\.\\./", "")
                  .replaceAll("\\.\\.\\\\", "");
          entryName = Paths.get(entryName).getFileName().toString();

          ZipEntry entry = new ZipEntry(entryName);
          zip.putNextEntry(entry);

          Files.copy(filePath, zip);

          zip.closeEntry();
        } else {
          log.warn("File not found on disk, skipping: {}", filePath);
        }
      }
      zip.finish();
    } catch (IOException e) {
      log.error("Error creating ZIP archive", e);
      throw new RuntimeException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UTILITY, 500)), e);
    }
  }

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
        newlyStoredFiles = storeFiles(newFiles, ErrorAction.UPDATE);
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

      Map<String, Object> resp = dbService.update("library", score.getId(), score);

      if (resp == null || !resp.containsKey("rev")) {
        throw new RuntimeException(
            String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UPDATE, 500)));
      }

      for (File oldFile : filesToPhysicallyDelete) {
        deleteFile(oldFile.getId() + "." + oldFile.getExtension(), ErrorAction.UPDATE);
      }

      try {
        sseService.broadcastRefresh("SCORES");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast SCORES refresh: ", ex);
      }

      return (String) resp.get("rev");
    } catch (Exception e) {
      log.error("Update failed. Rolling back new uploads.", e);
      for (File newFile : newlyStoredFiles) {
        deleteFile(newFile.getId() + "." + newFile.getExtension(), ErrorAction.UPDATE);
      }

      if (e instanceof RuntimeException)
        throw new RuntimeException(
            String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UPDATE, 500)), e);
      throw new RuntimeException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UPDATE, 500)), e);
    }
  }

  private List<File> storeFiles(List<MultipartFile> files, ErrorAction action) throws IOException {
    if (files == null || files.isEmpty()) return List.of();

    List<File> storedFiles = new ArrayList<>();
    List<Path> physicalPaths = new ArrayList<>();
    Path root = Paths.get(scoresDir);

    try {
      Files.createDirectories(root);

      for (MultipartFile file : files) {
        if (file.getSize() > MAX_FILE_SIZE) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.LIBRARY.createCode(action, 400)));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) continue;

        String id = UUID.randomUUID().toString();
        int dotIndex = originalName.lastIndexOf('.');
        String extensionWithDot = (dotIndex == -1) ? "" : originalName.substring(dotIndex);
        String extensionOnly = extensionWithDot.replace(".", "");

        Path targetPath = root.resolve(id + extensionWithDot);

        Files.copy(file.getInputStream(), targetPath);
        physicalPaths.add(targetPath);

        storedFiles.add(
            File.builder()
                .id(id)
                .originalName(originalName)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .extension(extensionOnly)
                .build());
      }
    } catch (Exception e) {
      log.error("Internal file storage failed. Cleaning up partial uploads...", e);
      for (Path path : physicalPaths) {
        try {
          Files.deleteIfExists(path);
        } catch (IOException cleanupEx) {
          log.warn("Failed to clean up partial upload: {}", path, cleanupEx);
        }
      }

      throw new RuntimeException(String.valueOf(ErrorDomain.LIBRARY.createCode(action, 500)), e);
    }
    return storedFiles;
  }

  private void deleteFile(String fileName, ErrorAction action) {
    Path filePath = Paths.get(scoresDir, fileName);

    if (!Files.exists(filePath)) {
      log.info("File not found on disk while deleting, skipping: {}", filePath);
      return;
    }

    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(ErrorDomain.LIBRARY.createCode(action, 500)), e);
    }
  }

  private boolean existsInLibrary(String scoreId, String title, String artist) {
    Map<String, Object> query =
        Map.of("selector", Map.of("scoreId", scoreId, "title", title, "artist", artist));
    List<Score> result = dbService.findByQuery("library", query, Score.class);

    return result != null && !result.isEmpty();
  }
}
