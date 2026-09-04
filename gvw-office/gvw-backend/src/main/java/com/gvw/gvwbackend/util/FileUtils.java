package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.StoredFile;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Utility component for handling file operations including storing, deleting, resolving, and
 * streaming files as ZIP archives.
 */
@Component
public class FileUtils {
  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  /** The maximum allowed file size for uploads in bytes (20 MB). */
  public static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

  /**
   * Stores uploaded files and creates corresponding file metadata objects.
   *
   * <p>Generates unique identifiers for files, stores them on disk, and collects metadata such as
   * MIME type, size, and extension.
   *
   * <p>If storing fails, already persisted files from the current operation are removed.
   *
   * @param files uploaded files
   * @param filesDir directory in which the uploaded files should be stored
   * @param domain error domain used for generating error codes
   * @param action action context used for generating error codes
   * @return metadata list of successfully stored files
   * @throws BadRequestException if a file exceeds the maximum allowed size
   * @throws RuntimeException if an I/O or unexpected error occurs during file storage
   */
  public List<File> storeFiles(
      List<MultipartFile> files, String filesDir, ErrorDomain domain, ErrorAction action) {
    if (files == null || files.isEmpty()) return List.of();

    List<com.gvw.gvwbackend.model.File> storedFiles = new ArrayList<>();
    List<Path> physicalPaths = new ArrayList<>();

    log.debug("Storing {} uploaded files", files.size());

    try {
      for (MultipartFile file : files) {
        Optional<StoredFile> storedFile = storeFile(file, filesDir, domain, action);
        if (storedFile.isEmpty()) continue;

        StoredFile stored = storedFile.get();

        physicalPaths.add(stored.path());

        log.debug("Successfully stored {} files", storedFiles.size());

        storedFiles.add(
            File.builder()
                .id(stored.id())
                .originalName(stored.originalName())
                .mimeType(file.getContentType())
                .size(file.getSize())
                .extension(stored.extension())
                .build());
      }
    } catch (BadRequestException e) {
      cleanUp(physicalPaths, e);
      throw e;
    } catch (Exception e) {
      cleanUp(physicalPaths, e);
      throw new RuntimeException(String.valueOf(domain.createCode(action, 500)), e);
    }
    return storedFiles;
  }

  /**
   * Stores a single uploaded file on the file system and generates a unique identifier.
   *
   * @param file the multipart file to be stored
   * @param filesDir directory where the file should be saved
   * @param domain error domain used for generating error codes
   * @param action action context used for generating error codes
   * @return an {@link Optional} containing {@link StoredFile} with file metadata if successful, or
   *     {@link Optional#empty()} if the file or its original filename is null/blank
   * @throws BadRequestException if the file exceeds {@link #MAX_FILE_SIZE}
   * @throws RuntimeException if an I/O or unexpected error occurs during storage
   */
  public Optional<StoredFile> storeFile(
      MultipartFile file, String filesDir, ErrorDomain domain, ErrorAction action) {
    if (file == null) return Optional.empty();

    Path root = Paths.get(filesDir);
    Path targetPath = null;

    String id = UUID.randomUUID().toString();

    try {
      Files.createDirectories(root);

      if (file.getSize() > MAX_FILE_SIZE) {
        throw new BadRequestException(String.valueOf(domain.createCode(action, 400)));
      }

      String originalName = file.getOriginalFilename();
      if (originalName == null || originalName.isBlank()) return Optional.empty();

      int dotIndex = originalName.lastIndexOf('.');
      String extensionWithDot = dotIndex == -1 ? "" : originalName.substring(dotIndex);
      String extension = dotIndex == -1 ? "" : originalName.substring(dotIndex + 1);

      targetPath = root.resolve(id + extensionWithDot);
      Files.copy(file.getInputStream(), targetPath);

      return Optional.of(new StoredFile(id, targetPath, originalName, extension));
    } catch (BadRequestException e) {
      if (targetPath != null) cleanUp(List.of(targetPath), e);
      throw e;
    } catch (Exception e) {
      if (targetPath != null) cleanUp(List.of(targetPath), e);
      throw new RuntimeException(String.valueOf(domain.createCode(action, 500)), e);
    }
  }

  /**
   * Deletes a file from the specified directory by its filename. Logs an error if the deletion
   * fails due to an I/O exception.
   *
   * @param fileName the name of the file to delete
   * @param filesDir the directory where the file is stored
   */
  public void deleteFile(String fileName, String filesDir) {
    Path filePath = Paths.get(filesDir, fileName);
    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      log.error("Failed to delete file: {}", filePath, e);
    }
  }

  /**
   * Deletes a file at the specified path location. Logs an error if the deletion fails due to an
   * I/O exception.
   *
   * @param filePath the {@link Path} of the file to delete
   */
  public void deleteFile(Path filePath) {
    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      log.error("Failed to delete file: {}", filePath, e);
    }
  }

  /**
   * Compresses a list of files into a ZIP archive and streams it directly to an {@link
   * OutputStream}. Sanitizes entry filenames to avoid path traversal issues.
   *
   * @param files list of {@link File} objects representing the metadata of files to be zipped
   * @param filesDir directory where the physical files are located
   * @param out the target output stream to write the ZIP archive to
   * @param domain error domain used for generating error codes on failure
   * @throws RuntimeException if an I/O error occurs while creating the ZIP archive
   */
  public void streamFilesAsZip(
      List<File> files, String filesDir, OutputStream out, ErrorDomain domain) {
    Path root = Paths.get(filesDir);

    try (ZipOutputStream zip = new ZipOutputStream(out)) {
      for (File file : files) {
        Path filePath = root.resolve(file.getId() + "." + file.getExtension());

        if (!Files.exists(filePath)) {
          log.warn("File nto found on disk, skipping: {}", filePath);
          continue;
        }

        String entryName =
            file.getOriginalName()
                .replaceAll("[\r\n]", "_")
                .replaceAll("\\.\\./", "")
                .replaceAll("\\.\\.\\\\", "");

        entryName = Paths.get(entryName).getFileName().toString();

        zip.putNextEntry(new ZipEntry(entryName));
        Files.copy(filePath, zip);
        zip.closeEntry();
      }

      zip.finish();
    } catch (IOException e) {
      log.error("Error creating ZIP archive", e);
      throw new RuntimeException(String.valueOf(domain.createCode(ErrorAction.UTILITY, 500)), e);
    }
  }

  /**
   * Resolves a file path while guarding against path traversal attacks.
   *
   * @param filename the name of the file to resolve
   * @param filesDir the base directory path
   * @param domain error domain used for generating error codes
   * @param action action context used for generating error codes
   * @param resource optional error resource context used for detailed error codes
   * @return normalized {@link Path} object targeting the file
   * @throws BadRequestException if the filename is invalid or attempts path traversal
   */
  public Path resolveFile(
      String filename,
      String filesDir,
      ErrorDomain domain,
      ErrorAction action,
      ErrorResource resource) {
    long errorCode =
        resource != null
            ? domain.createCode(action, 400, resource)
            : domain.createCode(action, 400);

    if (filename == null
        || filename.isBlank()
        || filename.contains("..")
        || filename.contains("/")) {
      throw new BadRequestException(String.valueOf(errorCode));
    }

    Path root = Paths.get(filesDir).toAbsolutePath().normalize();
    Path file = root.resolve(filename).normalize();

    if (!file.startsWith(root)) {
      throw new BadRequestException(String.valueOf(errorCode));
    }

    return file;
  }

  /**
   * Helper method to clean up created files when an error occurs during processing.
   *
   * @param paths list of file {@link Path} instances to be deleted
   * @param e the exception that triggered the cleanup process
   */
  private void cleanUp(List<Path> paths, Exception e) {
    log.error("Internal file storage failed. Cleaning up partial uploads...", e);
    for (Path path : paths) {
      try {
        Files.deleteIfExists(path);
      } catch (IOException cleanupEx) {
        log.warn("Failed to clean up partial upload: {}", path, cleanupEx);
      }
    }
  }
}
