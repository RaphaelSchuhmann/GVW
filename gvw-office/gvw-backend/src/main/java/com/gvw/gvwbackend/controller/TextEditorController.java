package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.response.LinkMetadataResponseDTO;
import com.gvw.gvwbackend.model.AttachmentResource;
import com.gvw.gvwbackend.service.HelpCenterService;
import com.gvw.gvwbackend.service.ReportService;
import com.gvw.gvwbackend.service.TextEditorService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/editor")
public class TextEditorController {
  private final TextEditorService editorService;
  private final ReportService reportService;
  private final HelpCenterService helpCenterService;

  public TextEditorController(
      TextEditorService editorService,
      ReportService reportService,
      HelpCenterService helpCenterService) {
    this.editorService = editorService;
    this.reportService = reportService;
    this.helpCenterService = helpCenterService;
  }

  @GetMapping("/assets/{feature}/{documentId}/{filename}")
  public ResponseEntity<Resource> getDocumentAsset(
      @PathVariable String feature,
      @PathVariable String documentId,
      @PathVariable String filename) {
    if ("reports".equalsIgnoreCase(feature)) {
      reportService.verifyAssetOwnership(documentId, filename);
    } else if ("help".equalsIgnoreCase(feature)) {
      helpCenterService.verifyAssetOwnership(documentId, filename);
    } else {
      return ResponseEntity.badRequest().build();
    }

    AttachmentResource asset = editorService.getAssetFile(filename);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(asset.getContentType()))
        .body(asset.getResource());
  }

  @GetMapping("/resolve")
  public LinkMetadataResponseDTO resolveLink(@RequestParam("url") String url) {
    return editorService.resolveUrl(url);
  }
}
