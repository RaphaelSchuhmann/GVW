package com.gvw.gvwbackend.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddScoreRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateScoreRequestDTO;
import com.gvw.gvwbackend.dto.response.ScoresResponseDTO;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.File;
import com.gvw.gvwbackend.model.Score;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class LibraryServiceTest {
  @Mock private DbService dbService;
  @Mock private SseService sseService;

  @InjectMocks private LibraryService libraryService;

  @TempDir Path tempDir;

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(libraryService, "scoresDir", tempDir.toString());
  }

  @Test
  void testGetAllScoresReturnsMappedDTOs() {
    Map<String, Object> raw =
        Map.of(
            "id", "1",
            "scoreId", "S1",
            "title", "Title",
            "artist", "Artist",
            "type", "PDF",
            "voices", List.of("S", "A"),
            "voiceCount", 2,
            "files", List.of());

    when(dbService.findAll("library")).thenReturn(List.of(raw));

    ScoresResponseDTO result = libraryService.getAllScores();

    assertThat(result.data().size()).isEqualTo(1);
    assertThat(result.data().getFirst().title()).isEqualTo("Title");
  }

  @Test
  void testCreateScoreSuccess() throws Exception {
    AddScoreRequestDTO request =
        new AddScoreRequestDTO("S1", "Title", "Artist", "PDF", List.of("S"), 1);

    MultipartFile file = mock(MultipartFile.class);
    when(file.getOriginalFilename()).thenReturn("test.pdf");
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream("data".getBytes()));

    when(dbService.findByQuery(any(), any(), eq(Score.class))).thenReturn(List.of()); // no conflict

    libraryService.createScore(request, List.of(file));

    verify(dbService).insert(eq("library"), any(Score.class));
  }

  @Test
  void testCreateScoreThrowsConflictWhenExists() {
    AddScoreRequestDTO request =
        new AddScoreRequestDTO("S1", "Title", "Artist", "PDF", List.of("t1"), 1);

    when(dbService.findByQuery(any(), any(), eq(Score.class))).thenReturn(List.of(new Score()));

    assertThatThrownBy(() -> libraryService.createScore(request, List.of()))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void testDeleteScoreSuccess() {
    Score score = new Score();
    score.setId("1");
    score.setRev("rev1");
    score.setFiles(List.of());

    when(dbService.findById("library", "1", Score.class)).thenReturn(score);

    libraryService.deleteScore("1");

    verify(dbService).delete("library", "1", "rev1");
  }

  @Test
  void testDeleteScoreThrowsNotFound() {
    when(dbService.findById("library", "1", Score.class)).thenReturn(null);

    assertThatThrownBy(() -> libraryService.deleteScore("1")).isInstanceOf(NotFoundException.class);
  }

  @Test
  void testUpdateScoreSuccess() throws Exception {
    Score existing = new Score();
    existing.setId("1");
    existing.setFiles(new ArrayList<>());

    when(dbService.findById("library", "1", Score.class)).thenReturn(existing);
    when(dbService.update(eq("library"), eq("1"), any(Score.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    UpdateScoreRequestDTO request =
        new UpdateScoreRequestDTO(
            "1", "S1", "NewTitle", "Artist", "PDF", List.of("t1"), 1, "1-rev");

    libraryService.updateScore(request, List.of(), List.of());

    verify(dbService).update(eq("library"), eq("1"), any(Score.class));
  }

  @Test
  void testStreamFilesAsZipWritesZip() throws Exception {
    Path file = tempDir.resolve("file1.pdf");
    Files.write(file, "content".getBytes());

    File f =
        File.builder()
            .id("file1")
            .originalName("file1.pdf")
            .mimeType("application/pdf")
            .size(10)
            .extension("pdf")
            .build();

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    libraryService.streamFilesAsZip(List.of(f), out);

    assertThat(out.toByteArray()).isNotEmpty();
  }
}
