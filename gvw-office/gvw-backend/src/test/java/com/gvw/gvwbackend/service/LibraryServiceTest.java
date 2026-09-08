package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddScoreRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateScoreRequestDTO;
import com.gvw.gvwbackend.dto.response.FullScoreResponseDTO;
import com.gvw.gvwbackend.dto.response.ScoreResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.Score;
import com.gvw.gvwbackend.util.FileUtils;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest {

  @Mock private DbService dbService;

  @Mock private SseService sseService;

  @Mock private FileUtils fileUtils;

  @InjectMocks private LibraryService libraryService;

  private Score score;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(libraryService, "scoresDir", "./api-data/scores");

    score =
        Score.builder()
            .id("score-1")
            .rev("1-abc")
            .scoreId("SCORE-001")
            .title("Test Score")
            .artist("Test Artist")
            .type("SATB")
            .voices(List.of("Soprano", "Alto", "Tenor", "Bass"))
            .voiceCount(4)
            .files(new ArrayList<>())
            .build();
  }

  @Test
  void getAllScores_Success() {
    when(dbService.findAll("library", Score.class)).thenReturn(List.of(score));

    List<ScoreResponseDTO> result = libraryService.getAllScores();

    assertEquals(1, result.size());
    assertEquals("score-1", result.getFirst().id());
    assertEquals("Test Score", result.getFirst().title());
  }

  @Test
  void getAllScores_Empty_ReturnsEmptyList() {
    when(dbService.findAll("library", Score.class)).thenReturn(List.of());

    List<ScoreResponseDTO> result = libraryService.getAllScores();

    assertTrue(result.isEmpty());
  }

  @Test
  void getFullScore_Success() {
    when(dbService.findById("library", "score-1", Score.class)).thenReturn(score);

    FullScoreResponseDTO result = libraryService.getFullScore("score-1");

    assertEquals("score-1", result.id());
    assertEquals("Test Score", result.title());
  }

  @Test
  void getFullScore_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> libraryService.getFullScore(null));
  }

  @Test
  void getFullScore_NotFound_ThrowsNotFound() {
    when(dbService.findById("library", "non-existent", Score.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> libraryService.getFullScore("non-existent"));
  }

  @Test
  void checkScore_Success() {
    when(dbService.findById("library", "score-1", Score.class)).thenReturn(score);

    assertDoesNotThrow(() -> libraryService.checkScore("score-1"));
  }

  @Test
  void checkScore_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> libraryService.checkScore(null));
  }

  @Test
  void checkScore_NotFound_ThrowsNotFound() {
    when(dbService.findById("library", "non-existent", Score.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> libraryService.checkScore("non-existent"));
  }

  @Test
  void createScore_Success() {
    AddScoreRequestDTO request =
        new AddScoreRequestDTO(
            "SCORE-002", "New Score", "New Artist", "SATB", List.of("Soprano"), 1);
    when(dbService.findByQuery(
            "library",
            Map.of(
                "selector",
                Map.of("scoreId", "SCORE-002", "title", "New Score", "artist", "New Artist")),
            Score.class))
        .thenReturn(List.of());
    when(fileUtils.storeFiles(
            isNull(), eq("./api-data/scores"), eq(ErrorDomain.LIBRARY), eq(ErrorAction.CREATE)))
        .thenReturn(List.of());

    libraryService.createScore(request, null);

    verify(dbService).insert(eq("library"), any(Score.class));
    verify(sseService).sendRefresh("SCORES");
  }

  @Test
  void createScore_Duplicate_ThrowsConflict() {
    AddScoreRequestDTO request =
        new AddScoreRequestDTO(
            "SCORE-001", "Test Score", "Test Artist", "SATB", List.of("Soprano"), 1);
    when(dbService.findByQuery(
            "library",
            Map.of(
                "selector",
                Map.of("scoreId", "SCORE-001", "title", "Test Score", "artist", "Test Artist")),
            Score.class))
        .thenReturn(List.of(score));

    assertThrows(ConflictException.class, () -> libraryService.createScore(request, null));
  }

  @Test
  void deleteScore_Success() {
    when(dbService.findById("library", "score-1", Score.class)).thenReturn(score);

    libraryService.deleteScore("score-1");

    verify(dbService).delete("library", "score-1", "1-abc");
    verify(sseService).sendRefresh("SCORES");
  }

  @Test
  void deleteScore_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> libraryService.deleteScore(null));
  }

  @Test
  void deleteScore_NotFound_ThrowsNotFound() {
    when(dbService.findById("library", "non-existent", Score.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> libraryService.deleteScore("non-existent"));
  }

  @Test
  void streamFilesAsZip_Success() {
    OutputStream out = mock(OutputStream.class);

    assertDoesNotThrow(() -> libraryService.streamFilesAsZip(score.getFiles(), out));
    verify(fileUtils)
        .streamFilesAsZip(eq(score.getFiles()), anyString(), eq(out), eq(ErrorDomain.LIBRARY));
  }

  @Test
  void updateScore_Success() {
    UpdateScoreRequestDTO request =
        new UpdateScoreRequestDTO(
            "score-1",
            "SCORE-001",
            "Updated Title",
            "Test Artist",
            "SATB",
            List.of("Soprano"),
            1,
            "1-abc");
    when(dbService.findById("library", "score-1", Score.class)).thenReturn(score);
    when(dbService.update("library", "score-1", score)).thenReturn("2-def");

    String result = libraryService.updateScore(request, null, null);

    assertEquals("2-def", result);
    verify(sseService).sendRefresh("SCORES");
  }

  @Test
  void updateScore_NotFound_ThrowsNotFound() {
    UpdateScoreRequestDTO request =
        new UpdateScoreRequestDTO(
            "non-existent", "SCORE-001", "Title", "Artist", "SATB", List.of("Soprano"), 1, "1-abc");
    when(dbService.findById("library", "non-existent", Score.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> libraryService.updateScore(request, null, null));
  }
}
