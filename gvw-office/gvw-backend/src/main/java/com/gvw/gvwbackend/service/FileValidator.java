package com.gvw.gvwbackend.service;

import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploaded files based on their file extensions.
 *
 * <p>This component is used to reject unsupported file types before they are
 * stored on the server. Only explicitly allowed extensions are accepted.
 *
 * <p>This validation only checks the filename extension and should not be
 * considered a complete file security solution. Additional validation such as
 * size limits, storage isolation, and content handling should be performed
 * by the consuming service.
 */
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

  /**
   * Checks whether an uploaded file has an allowed extension.
   *
   * <p>The extension is extracted from the original filename and compared
   * against the configured allowlist.
   *
   * @param file uploaded file to validate
   * @return {@code true} if the file extension is allowed, otherwise {@code false}
   */
  public boolean isSafe(MultipartFile file) {
    if (file == null) return false;

    String fileName = file.getOriginalFilename();
    if (fileName == null || !fileName.contains(".")) return false;

    String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    return ALLOWED_EXTENSIONS.contains(extension);
  }
}
