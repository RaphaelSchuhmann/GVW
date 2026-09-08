package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.StoredFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileUtilsTest {

  @InjectMocks private FileUtils fileUtils;

  private static final String BASE_DIR = "./api-data/test";

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    // No setup needed as FileUtils is a utility component
  }

  @Test
  void storeFiles_Success() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
    when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
    when(mockFile.getSize()).thenReturn(12L);
    when(mockFile.getContentType()).thenReturn("application/pdf");

    List<File> result =
        fileUtils.storeFiles(List.of(mockFile), BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.CREATE);

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
                List.of(mockFile), BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.CREATE));
  }

  @Test
  void storeFiles_EmptyList_ReturnsEmptyList() {
    List<File> result =
        fileUtils.storeFiles(null, BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void storeFile_Success() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.pdf");
    when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
    when(mockFile.getSize()).thenReturn(12L);

    Optional<StoredFile> result =
        fileUtils.storeFile(mockFile, BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertTrue(result.isPresent());
    assertEquals("test.pdf", result.get().originalName());
  }

  @Test
  void storeFile_NullFile_ReturnsEmpty() {
    Optional<StoredFile> result =
        fileUtils.storeFile(null, BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.CREATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void storeFile_FileTooLarge_ThrowsBadRequest() throws IOException {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getSize()).thenReturn(25L * 1024 * 1024); // 25 MB

    assertThrows(
        BadRequestException.class,
        () -> fileUtils.storeFile(mockFile, BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.CREATE));
  }

  @Test
  void deleteFile_Success() throws IOException {
    assertDoesNotThrow(() -> fileUtils.deleteFile("test.pdf", BASE_DIR));
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
        fileUtils.resolveFile("test.pdf", BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.UTILITY, null);

    assertNotNull(result);
  }

  @Test
  void resolveFile_PathTraversal_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            fileUtils.resolveFile(
                "../test.pdf", BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.UTILITY, null));
  }

  @Test
  void resolveFile_NullFilename_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class,
        () ->
            fileUtils.resolveFile(null, BASE_DIR, ErrorDomain.LIBRARY, ErrorAction.UTILITY, null));
  }

  @Test
  void streamFilesAsZip_Success() throws IOException {
    File mockFile = mock(File.class);
    when(mockFile.getId()).thenReturn("file-1");
    when(mockFile.getExtension()).thenReturn("pdf");
    OutputStream out = mock(OutputStream.class);

    assertDoesNotThrow(
        () -> fileUtils.streamFilesAsZip(List.of(mockFile), BASE_DIR, out, ErrorDomain.LIBRARY));
  }
}
