package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddScoreRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateScoreRequestDTO;
import com.gvw.gvwbackend.dto.response.ScoresResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.model.Score;
import com.gvw.gvwbackend.service.DbService;
import com.gvw.gvwbackend.service.FileValidator;
import com.gvw.gvwbackend.service.LibraryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/library")
public class LibraryController {
  private final LibraryService libraryService;
  private final FileValidator fileValidator;
  private final DbService dbService;

  public LibraryController(
      LibraryService libraryService, FileValidator fileValidator, DbService dbService) {
    this.libraryService = libraryService;
    this.fileValidator = fileValidator;
    this.dbService = dbService;
  }

  @GetMapping("/all")
  public ScoresResponseDTO getAllScores() {
    return libraryService.getAllScores();
  }

  @GetMapping("/check/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void checkScore(@PathVariable String id) {
    libraryService.checkScore(id);
  }

  @PostMapping(value = "/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'LIBRARIAN')")
  @ErrorContext(
      domain = ErrorDomain.LIBRARY,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void createScore(
      @RequestPart("scoreData") @Valid AddScoreRequestDTO request,
      @RequestPart(value = "files", required = false) List<MultipartFile> files) {
    if (files != null) {
      for (MultipartFile file : files) {
        if (!fileValidator.isSafe(file)) {
          // 1399400
          throw new BadRequestException(
              String.valueOf(ErrorDomain.FILE_VALIDATOR.createCode(ErrorAction.UTILITY, 400)));
        }
      }
    }

    libraryService.createScore(request, files);
  }

  @DeleteMapping("/delete/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'LIBRARIAN')")
  public void deleteScore(@PathVariable String id) {
    libraryService.deleteScore(id);
  }

  @GetMapping("/{id}/files")
  public ResponseEntity<StreamingResponseBody> downloadScoreFiles(@PathVariable String id) {
    Score score = dbService.findById("library", id, Score.class);
    if (score == null)
      throw new NotFoundException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UTILITY, 404)));
    if (score.getFiles() == null || score.getFiles().isEmpty()) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.LIBRARY.createCode(ErrorAction.UTILITY, 404)));
    }

    String sanitizedTitle = score.getTitle().replaceAll("[\"\r\n]", "_");

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, "application/zip")
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedTitle + ".zip\"")
        .body(out -> libraryService.streamFilesAsZip(score.getFiles(), out));
  }

  @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER', 'LIBRARIAN')")
  @ErrorContext(
      domain = ErrorDomain.LIBRARY,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateScore(
      @RequestPart("scoreData") @Valid UpdateScoreRequestDTO request,
      @RequestPart(value = "files", required = false) List<MultipartFile> newFiles,
      @RequestParam(value = "removedFiles", required = false) List<String> removedFiles) {
    if (newFiles != null) {
      for (MultipartFile file : newFiles) {
        if (!fileValidator.isSafe(file)) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.FILE_VALIDATOR.createCode(ErrorAction.UTILITY, 400)));
        }
      }
    }
    String rev = libraryService.updateScore(request, newFiles, removedFiles);
    return Map.of("rev", rev);
  }
}
