package com.gvw.gvwbackend.service;

import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileValidator {
  private static final Set<String> ALLOWED_EXTENSIONS =
      Set.of(
          "pdf",
          "png",
          "jpg",
          "jpeg",
          "gif",
          "mp3",
          "wav",
          "midi",
          "mid",
          "xml",
          "musicxml",
          "mxl",
          "mscz",
          "mscx",
          "sib",
          "musx",
          "cap",
          "capx",
          "gp",
          "gp5",
          "gp3",
          "gp4",
          "gpx");

  public boolean isSafe(MultipartFile file) {
    String fileName = file.getOriginalFilename();
    if (fileName == null || !fileName.contains(".")) return false;

    String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    return ALLOWED_EXTENSIONS.contains(extension);
  }
}
