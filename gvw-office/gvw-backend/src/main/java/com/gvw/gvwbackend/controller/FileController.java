package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/file")
public class FileController {
  private final FileService fileService;

  public FileController(FileService fileService) {
    this.fileService = fileService;
  }

  @GetMapping("/download/{service}/{id}/zip")
  public ResponseEntity<StreamingResponseBody> streamFilesAsZip(
      @PathVariable String service,
      @PathVariable String id,
      @RequestAttribute("userId") String userId) {
    return fileService.streamFilesAsZip(service, id, userId);
  }

  @GetMapping("/{service}/{docId}/{id}")
  public ResponseEntity<StreamingResponseBody> loadFile(
      @PathVariable String service,
      @PathVariable String docId,
      @PathVariable String id,
      @RequestAttribute("userId") String userId) {
    return fileService.loadFile(service, docId, id, userId);
  }
}
