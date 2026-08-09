package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.response.LinkMetadataResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service responsible for handling text editor related operations.
 *
 * <p>Provides functionality for:
 *
 * <ul>
 *   <li>Managing uploaded editor assets
 *   <li>Resolving URL metadata for rich links
 *   <li>Synchronizing and cleaning unused block assets
 *   <li>Converting editor blocks into plain text
 *   <li>Searching through text documents
 * </ul>
 *
 * <p>Files are stored on the local filesystem and validated before being persisted. External URL
 * requests are restricted to prevent access to local or private network addresses.
 */
@Getter
@Service
public class TextEditorService {
  private static final Logger log = LoggerFactory.getLogger(TextEditorService.class);
  private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

  @Value("${editor.directory:./api-data/editor-assets}")
  private String filesDir;

  /**
   * Retrieves an editor asset file from disk.
   *
   * <p>Validates the filename to prevent path traversal attacks and resolves the file content type
   * before returning the resource.
   *
   * @param filename name of the asset file
   * @return file resource with detected content type
   * @throws BadRequestException if the filename is invalid
   * @throws NotFoundException if the file does not exist
   */
  public AttachmentResource getAssetFile(String filename) {
    if (filename == null
        || filename.isBlank()
        || filename.contains("..")
        || filename.contains("/")) {
      throw new BadRequestException(
          String.valueOf(
              ErrorDomain.TEXT_EDITOR.createCode(
                  ErrorAction.UTILITY, 400, ErrorResource.TEXT_EDITOR_CONTENT)));
    }

    Path filePath = Paths.get(filesDir, filename);
    File file = filePath.toFile();

    if (!file.exists()) {
      throw new NotFoundException(
          String.valueOf(
              ErrorDomain.TEXT_EDITOR.createCode(
                  ErrorAction.UTILITY, 404, ErrorResource.TEXT_EDITOR_CONTENT)));
    }

    try {
      String contentType = Files.probeContentType(filePath);
      return new AttachmentResource(
          file,
          (contentType == null || contentType.isBlank())
              ? "application/octet-stream"
              : contentType);
    } catch (IOException e) {
      throw new RuntimeException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(ErrorAction.UTILITY, 500)));
    }
  }

  /**
   * Resolves metadata for an external URL.
   *
   * <p>Fetches the target page, extracts its title and favicon, and returns the favicon as a Base64
   * data URL for use in rich links.
   *
   * <p>Only HTTPS URLs pointing to publicly accessible hosts are allowed.
   *
   * @param url URL to resolve
   * @return metadata containing page title and favicon data
   */
  public LinkMetadataResponseDTO resolveUrl(String url) {
    try {
      URI uri = new URI(url);
      if (!"https".equalsIgnoreCase(uri.getScheme())) {
        throw new BadRequestException(
            String.valueOf(
                ErrorDomain.TEXT_EDITOR.createCode(
                    ErrorAction.UTILITY, 400, ErrorResource.TEXT_EDITOR_CONTENT)));
      }

      String host = uri.getHost();
      if (host == null
          || Arrays.stream(InetAddress.getAllByName(host)).anyMatch(this::isBlockedAddress)) {
        throw new BadRequestException(
            String.valueOf(
                ErrorDomain.TEXT_EDITOR.createCode(
                    ErrorAction.UTILITY, 400, ErrorResource.TEXT_EDITOR_CONTENT)));
      }

      org.jsoup.nodes.Document doc =
          Jsoup.connect(uri.toString())
              .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
              .timeout(5000)
              .get();

      String title = doc.title().isBlank() ? url : doc.title();
      org.jsoup.nodes.Element iconElement =
          doc.head().select("link[rel~=(?i)^(shortcut|icon|apple-touch-icon)$]").first();
      String faviconUrl =
          (iconElement != null)
              ? iconElement.attr("abs:href")
              : uri.getScheme() + "://" + host + "/favicon.ico";

      byte[] imageBytes =
          Jsoup.connect(faviconUrl).ignoreContentType(true).timeout(3000).execute().bodyAsBytes();

      String base64Image = Base64.getEncoder().encodeToString(imageBytes);
      String dataUrl = "data:image/x-icon;base64," + base64Image;

      return new LinkMetadataResponseDTO(title, dataUrl);
    } catch (Exception e) {
      return new LinkMetadataResponseDTO(url, "");
    }
  }

  /**
   * Stores uploaded editor files on disk.
   *
   * <p>Each file receives a generated unique filename while preserving the original extension. The
   * returned map contains the relationship between original filenames and stored filenames.
   *
   * <p>If storage fails, all files written during the operation are removed.
   *
   * @param files uploaded files
   * @param action action context used for generating error codes
   * @return mapping of original filenames to stored filenames
   * @throws BadRequestException if a file exceeds the maximum allowed size
   */
  public Map<String, String> processUploadedFiles(List<MultipartFile> files, ErrorAction action) {
    if (files == null || files.isEmpty()) return Map.of();

    Map<String, String> filenames = new HashMap<>();
    List<Path> physicalPaths = new ArrayList<>();
    Path root = Paths.get(filesDir);

    try {
      Files.createDirectories(root);
      for (MultipartFile file : files) {
        if (file.getSize() > MAX_FILE_SIZE) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(action, 400)));
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) continue;

        String id = UUID.randomUUID().toString();
        int dotIndex = originalName.lastIndexOf(".");
        String extension = (dotIndex == -1) ? "" : originalName.substring(dotIndex);
        Path targetPath = root.resolve(id + extension);

        Files.copy(file.getInputStream(), targetPath);
        filenames.put(originalName, id + extension);
        physicalPaths.add(targetPath);
      }
      return filenames;
    } catch (Exception e) {
      physicalPaths.forEach(
          path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
          });
      if (e instanceof BadRequestException) throw (BadRequestException) e;
      throw new RuntimeException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(action, 500)), e);
    }
  }

  /**
   * Removes unused assets after updating editor content.
   *
   * <p>Compares asset references between the previous and updated blocks and deletes files that are
   * no longer referenced.
   *
   * @param oldBlocks previous editor content
   * @param newBlocks updated editor content
   * @param action action context used for generating error codes
   */
  public void synchronizeBlockAssets(
      List<TextEditorBlock> oldBlocks, List<TextEditorBlock> newBlocks, ErrorAction action) {
    Set<String> oldIds = extractFileIds(oldBlocks);
    Set<String> newIds = extractFileIds(newBlocks);

    oldIds.removeAll(newIds);
    for (String deadFile : oldIds) {
      deleteAssetFromDisk(deadFile, action);
    }
  }

  /**
   * Deletes all assets referenced by the provided editor blocks.
   *
   * @param blocks editor blocks containing asset references
   * @param action action context used for generating error codes
   */
  public void purgeAllBlockAssets(List<TextEditorBlock> blocks, ErrorAction action) {
    extractFileIds(blocks).forEach(id -> deleteAssetFromDisk(id, action));
  }

  /**
   * Extracts referenced asset identifiers from image blocks.
   *
   * @param content editor blocks to inspect
   * @return set of referenced file identifiers
   */
  public Set<String> extractFileIds(List<TextEditorBlock> content) {
    if (content == null || content.isEmpty()) return Set.of();
    Set<String> ids = new HashSet<>();

    for (TextEditorBlock block : content) {
      if (block.getType() == TextEditorBlockType.IMAGE && block.getData() != null) {
        ids.add(block.getData());
      }
    }

    return ids;
  }

  /**
   * Converts editor blocks into plain text.
   *
   * <p>Image blocks are ignored and all HTML content is sanitized before being added to the
   * resulting text.
   *
   * @param contents editor blocks
   * @return plain text representation of the content
   */
  public String convertBlocksToPlainText(List<TextEditorBlock> contents) {
    if (contents == null || contents.isEmpty()) return "";
    StringBuilder sb = new StringBuilder();

    for (TextEditorBlock block : contents) {
      if (block.getType() == TextEditorBlockType.IMAGE) continue;
      String data = block.getData();
      if (data == null || data.isEmpty()) continue;

      String cleanData = org.jsoup.Jsoup.clean(data, org.jsoup.safety.Safelist.none());
      if (!sb.isEmpty()) sb.append(" ");
      sb.append(cleanData);
    }

    return sb.toString();
  }

  /**
   * Deletes an editor asset from local storage.
   *
   * @param file filename of the asset to delete
   * @param action action context used for generating error codes
   * @throws RuntimeException if deletion fails
   */
  public void deleteAssetFromDisk(String file, ErrorAction action) {
    Path filePath = Paths.get(filesDir, file);
    try {
      Files.deleteIfExists(filePath);
    } catch (IOException e) {
      throw new RuntimeException(String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(action, 500)));
    }
  }

  /**
   * Checks whether an IP address points to a local or private network address.
   *
   * <p>Used to prevent server-side requests to internal resources when resolving external URLs.
   *
   * @param addr address to validate
   * @return {@code true} if the address is blocked
   */
  public boolean isBlockedAddress(InetAddress addr) {
    return addr.isAnyLocalAddress()
        || addr.isLoopbackAddress()
        || addr.isLinkLocalAddress()
        || addr.isSiteLocalAddress();
  }

  /**
   * Stores uploaded files and creates corresponding file metadata objects.
   *
   * <p>Generates unique identifiers for files, stores them on disk, and collects metadata such as
   * MIME type, size, and extension.
   *
   * <p>If storing fails, already persisted files from the current operation are removed.
   *
   * @param files uploaded files
   * @param action action context used for generating error codes
   * @return metadata of successfully stored files
   * @throws BadRequestException if a file exceeds the maximum allowed size
   */
  public List<com.gvw.gvwbackend.model.File> storeFiles(
      List<MultipartFile> files, ErrorAction action) {
    if (files == null || files.isEmpty()) return List.of();

    List<com.gvw.gvwbackend.model.File> storedFiles = new ArrayList<>();
    List<Path> physicalPaths = new ArrayList<>();
    Path root = Paths.get(filesDir);

    try {
      Files.createDirectories(root);

      for (MultipartFile file : files) {
        if (file.getSize() > MAX_FILE_SIZE) {
          throw new BadRequestException(
              String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(action, 400)));
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
            com.gvw.gvwbackend.model.File.builder()
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

      throw new RuntimeException(
          String.valueOf(ErrorDomain.TEXT_EDITOR.createCode(action, 500)), e);
    }
    return storedFiles;
  }

  /**
   * Calculates the estimated reading time of editor content.
   *
   * <p>The calculation assumes an average reading speed of 200 words per minute.
   *
   * @param content editor blocks
   * @return estimated reading time in minutes
   */
  public int getReadingTime(List<TextEditorBlock> content) {
    String plainText = convertBlocksToPlainText(content);

    List<String> words =
        Arrays.stream(plainText.split("\\s+")).filter(word -> !word.isEmpty()).toList();

    if (words.isEmpty()) return 0;
    return (words.size() + 199) / 200;
  }

  /**
   * Searches through multiple text documents for a given term.
   *
   * <p>Searches sanitized plain text content and returns matching documents together with a
   * surrounding text snippet.
   *
   * @param documents documents to search
   * @param input search term
   * @param <T> document type extending {@link TextDocument}
   * @return matching documents with context snippets
   */
  public <T extends TextDocument> List<TextDocumentSearchResult<T>> deepSearch(
      List<T> documents, String input) {
    String regex = "(?i)" + Pattern.quote(input);
    Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    List<TextDocumentSearchResult<T>> results = new ArrayList<>();

    for (T doc : documents) {
      String content = convertBlocksToPlainText(doc.getContents());
      if (content.isBlank()) continue;

      Matcher matcher = pattern.matcher(content);

      if (matcher.find()) {
        int start = matcher.start();
        int end = matcher.end();
        String snippet = extractSnippet(content, start, end);
        results.add(new TextDocumentSearchResult<>(doc, snippet));
      }
    }

    return results;
  }

  /**
   * Extracts a text snippet around a search match.
   *
   * @param content full document text
   * @param hitStart start index of the match
   * @param hitEnd end index of the match
   * @return shortened text excerpt around the match
   */
  private String extractSnippet(String content, int hitStart, int hitEnd) {
    int start;
    int end;

    if (hitStart <= 50) {
      start = 0;
    } else {
      start = hitStart - 50;
    }

    end = Math.min(hitEnd + 50, content.length());

    return content.substring(start, end);
  }
}
