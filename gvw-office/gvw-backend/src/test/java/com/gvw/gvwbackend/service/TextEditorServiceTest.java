package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.response.LinkMetadataResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.AttachmentResource;
import com.gvw.gvwbackend.model.StoredFile;
import com.gvw.gvwbackend.model.TextDocument;
import com.gvw.gvwbackend.model.TextDocumentSearchResult;
import com.gvw.gvwbackend.model.TextEditorBlock;
import com.gvw.gvwbackend.model.TextEditorBlockType;
import com.gvw.gvwbackend.util.FileUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class TextEditorServiceTest {

  @Mock private FileUtils fileUtils;

  @InjectMocks private TextEditorService textEditorService;

  private static final String EDITOR_DIR = "./api-data/editor-assets";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(textEditorService, "editorAssetsDir", EDITOR_DIR);
  }

  @Test
  void getAssetFile_Success() {
    Path mockPath = mock(Path.class);
    File mockFile = mock(File.class);
    when(fileUtils.resolveFile(anyString(), anyString(), any(), any(), any())).thenReturn(mockPath);
    when(mockPath.toFile()).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(true);

    AttachmentResource result = textEditorService.getAssetFile("test.png");

    assertNotNull(result);
  }

  @Test
  void getAssetFile_InvalidFilename_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> textEditorService.getAssetFile("../test.png"));
  }

  @Test
  void getAssetFile_NullFilename_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> textEditorService.getAssetFile(null));
  }

  @Test
  void getAssetFile_NotFound_ThrowsNotFound() {
    Path mockPath = mock(Path.class);
    File mockFile = mock(File.class);
    when(fileUtils.resolveFile(anyString(), anyString(), any(), any(), any())).thenReturn(mockPath);
    when(mockPath.toFile()).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(false);

    assertThrows(NotFoundException.class, () -> textEditorService.getAssetFile("test.png"));
  }

  @Test
  void resolveUrl_Success() {
    LinkMetadataResponseDTO result = textEditorService.resolveUrl("https://example.com");

    assertNotNull(result);
  }

  @Test
  void resolveUrl_NonHttps_ThrowsBadRequest() {
    assertThrows(
        BadRequestException.class, () -> textEditorService.resolveUrl("http://example.com"));
  }

  @Test
  void processUploadedFiles_Success() {
    MultipartFile mockFile = mock(MultipartFile.class);
    StoredFile storedFile = new StoredFile("id-1", mock(Path.class), "test.png", ".png");
    when(fileUtils.storeFile(any(), anyString(), any(), any())).thenReturn(Optional.of(storedFile));

    var result = textEditorService.processUploadedFiles(List.of(mockFile), ErrorAction.CREATE);

    assertEquals(1, result.size());
  }

  @Test
  void processUploadedFiles_Empty_ReturnsEmptyMap() {
    var result = textEditorService.processUploadedFiles(null, ErrorAction.CREATE);

    assertTrue(result.isEmpty());
  }

  @Test
  void synchronizeBlockAssets_Success() {
    TextEditorBlock oldBlock = new TextEditorBlock();
    oldBlock.setType(TextEditorBlockType.IMAGE);
    oldBlock.setData("file-1.png");

    TextEditorBlock newBlock = new TextEditorBlock();
    newBlock.setType(TextEditorBlockType.TEXT);
    newBlock.setData("Some text");

    assertDoesNotThrow(
        () ->
            textEditorService.synchronizeBlockAssets(
                List.of(oldBlock), List.of(newBlock), ErrorAction.UPDATE));
    verify(fileUtils).deleteFile(eq("file-1.png"), anyString());
  }

  @Test
  void purgeAllBlockAssets_Success() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.IMAGE);
    block.setData("file-1.png");

    assertDoesNotThrow(() -> textEditorService.purgeAllBlockAssets(List.of(block)));
    verify(fileUtils).deleteFile(eq("file-1.png"), anyString());
  }

  @Test
  void extractFileIds_Success() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.IMAGE);
    block.setData("file-1.png");

    var result = textEditorService.extractFileIds(List.of(block));

    assertEquals(1, result.size());
    assertTrue(result.contains("file-1.png"));
  }

  @Test
  void extractFileIds_Empty_ReturnsEmptySet() {
    var result = textEditorService.extractFileIds(null);

    assertTrue(result.isEmpty());
  }

  @Test
  void convertBlocksToPlainText_Success() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.TEXT);
    block.setData("<p>Test content</p>");

    String result = textEditorService.convertBlocksToPlainText(List.of(block));

    assertEquals("Test content", result);
  }

  @Test
  void convertBlocksToPlainText_IgnoresImages() {
    TextEditorBlock textBlock = new TextEditorBlock();
    textBlock.setType(TextEditorBlockType.TEXT);
    textBlock.setData("Text");

    TextEditorBlock imageBlock = new TextEditorBlock();
    imageBlock.setType(TextEditorBlockType.IMAGE);
    imageBlock.setData("file.png");

    String result = textEditorService.convertBlocksToPlainText(List.of(textBlock, imageBlock));

    assertEquals("Text", result);
  }

  @Test
  void isBlockedAddress_Localhost_ReturnsTrue() {
    java.net.InetAddress localhost = java.net.InetAddress.getLoopbackAddress();

    boolean result = textEditorService.isBlockedAddress(localhost);

    assertTrue(result);
  }

  @Test
  void getReadingTime_Success() {
    TextEditorBlock block = new TextEditorBlock();
    block.setType(TextEditorBlockType.TEXT);
    block.setData("word1 word2 word3");

    int result = textEditorService.getReadingTime(List.of(block));

    assertEquals(1, result); // 3 words / 200 = 0.015, rounded to 1
  }

  @Test
  void getReadingTime_Empty_ReturnsZero() {
    int result = textEditorService.getReadingTime(List.of());

    assertEquals(0, result);
  }

  @Test
  void deepSearch_Success() {
    TextDocument doc = mock(TextDocument.class);
    when(doc.getContents())
        .thenReturn(
            List.of(new TextEditorBlock("block-id", TextEditorBlockType.TEXT, "test content")));

    List<TextDocumentSearchResult<TextDocument>> result =
        textEditorService.deepSearch(List.of(doc), "test");

    assertNotNull(result);
  }

  @Test
  void deepSearch_NoMatch_ReturnsEmptyList() {
    TextDocument doc = mock(TextDocument.class);
    when(doc.getContents())
        .thenReturn(
            List.of(new TextEditorBlock("block-id", TextEditorBlockType.TEXT, "other content")));

    List<TextDocumentSearchResult<TextDocument>> result =
        textEditorService.deepSearch(List.of(doc), "test");

    assertTrue(result.isEmpty());
  }
}
