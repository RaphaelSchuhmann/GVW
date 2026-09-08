package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.StoredFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileUtilsTest {

  @InjectMocks private FileUtils fileUtils;

  @TempDir Path tempDir;

  private String baseDir() {
    return tempDir.toString();
  }

  @Test
  void storeFiles_Success() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
    when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
    when(mockFile.getSize()).thenReturn(12L);
    when(mockFile.getContentType()).thenReturn("application/pdf");

    List<File> result =
        fileUtils.storeFiles(List.of(mockFile), baseDir(), ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertEquals(1, result.size());
    assertEquals("test.pdf", result.getFirst().getOriginalName());
  }

  @Test
  void storeFiles_FileTooLarge_ThrowsBadRequest() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getSize()).thenReturn(25L * 1024 * 1024); // 25 MB

    assertThrows(
        BadRequestException.class,
        () ->
            fileUtils.storeFiles(
                List.of(mockFile), baseDir(), ErrorDomain.LIBRARY, ErrorAction.CREATE));
  }

  @Test
  void storeFiles_EmptyList_ReturnsEmptyList() {
    List<File> result =
        fileUtils.storeFiles(null, baseDir(), ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void storeFile_Success() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
    when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
    when(mockFile.getSize()).thenReturn(12L);

    Optional<StoredFile> result =
        fileUtils.storeFile(mockFile, baseDir(), ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertTrue(result.isPresent());
    assertEquals("test.pdf", result.get().originalName());
  }

  @Test
  void storeFile_NullFile_ReturnsEmpty() {
    Optional<StoredFile> result =
        fileUtils.storeFile(null, baseDir(), ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void storeFile_FileTooLarge_ThrowsBadRequest() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getSize()).thenReturn(25L * 1024 * 1024); // 25 MB

    assertThrows(
        BadRequestException.class,
        () -> fileUtils.storeFile(mockFile, baseDir(), ErrorDomain.LIBRARY, ErrorAction.CREATE));
  }

  @Test
  void deleteFile_Success() throws IOException {
    assertDoesNotThrow(() -> fileUtils.deleteFile("test.pdf", baseDir()));
  }

  @Test
  void deleteFile_Path_Success() throws IOException {
    Path file = Files.createFile(tempDir.resolve("test.txt"));

    assertTrue(Files.exists(file));

    assertDoesNotThrow(() -> fileUtils.deleteFile(file));

    assertFalse(Files.exists(file));
  }

  @Test
  void resolveFile_Success() throws IOException {
    Path result =
        fileUtils.resolveFile(
            "test.pdf", baseDir(), ErrorDomain.LIBRARY, ErrorAction.UTILITY, null);

    assertNotNull(result);
  }

  @Test
  void resolveFile_PathTraversal_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            fileUtils.resolveFile(
                "../test.pdf", baseDir(), ErrorDomain.LIBRARY, ErrorAction.UTILITY, null));
  }

  @Test
  void resolveFile_NullFilename_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            fileUtils.resolveFile(null, baseDir(), ErrorDomain.LIBRARY, ErrorAction.UTILITY, null));
  }

  @Test
  void streamFilesAsZip_Success() throws IOException {
    Path realFile = Files.createFile(tempDir.resolve("file-1.pdf"));
    Files.write(realFile, "test content".getBytes());

    File file = new File();
    file.setId("file-1");
    file.setExtension("pdf");
    file.setOriginalName("file-1.pdf");

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    fileUtils.streamFilesAsZip(List.of(file), tempDir.toString(), out, ErrorDomain.LIBRARY);

    byte[] zipBytes = out.toByteArray();
    assertTrue(zipBytes.length > 0);

    try (ZipInputStream zipIn = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {

      ZipEntry entry = zipIn.getNextEntry();

      assertNotNull(entry);
      assertEquals("file-1.pdf", entry.getName());
    }
  }
}
