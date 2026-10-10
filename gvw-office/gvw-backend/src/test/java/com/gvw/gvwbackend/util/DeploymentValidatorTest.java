package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.model.Deployment;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeploymentValidatorTest {

  @Mock private HttpClient httpClient;

  private DeploymentValidator validator;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    fixedClock = Clock.fixed(Instant.parse("2024-01-15T12:00:00Z"), ZoneOffset.UTC);
    validator = new DeploymentValidator(httpClient, fixedClock);
  }

  @Test
  void isTodayAndAlreadyStarted_NullDate_ReturnsFalse() {
    assertFalse(validator.isTodayAndAlreadyStarted(null, LocalTime.NOON));
  }

  @Test
  void isTodayAndAlreadyStarted_NullTime_ReturnsFalse() {
    String todayIso = Instant.now(fixedClock).atZone(ZoneOffset.UTC).toString();
    assertFalse(validator.isTodayAndAlreadyStarted(todayIso, null));
  }

  @Test
  void isTodayAndAlreadyStarted_BothNull_ReturnsFalse() {
    assertFalse(validator.isTodayAndAlreadyStarted(null, null));
  }

  @Test
  void isTodayAndAlreadyStarted_FutureDate_ReturnsFalse() {
    String futureDate =
        LocalDate.now(fixedClock.withZone(ZoneOffset.UTC))
            .plusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toString();
    assertFalse(validator.isTodayAndAlreadyStarted(futureDate, LocalTime.NOON));
  }

  @Test
  void isTodayAndAlreadyStarted_PastDate_ReturnsTrue() {
    String pastDate =
        LocalDate.now(fixedClock.withZone(ZoneOffset.UTC))
            .minusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toString();
    assertTrue(validator.isTodayAndAlreadyStarted(pastDate, LocalTime.NOON));
  }

  @Test
  void isTodayAndAlreadyStarted_TodayAndTimeNotPassed_ReturnsFalse() {
    String todayIso = Instant.now(fixedClock).toString();
    LocalTime futureTime = LocalTime.now(fixedClock.withZone(ZoneOffset.UTC)).plusHours(1);
    assertFalse(validator.isTodayAndAlreadyStarted(todayIso, futureTime));
  }

  @Test
  void isTodayAndAlreadyStarted_TodayAndTimePassed_ReturnsTrue() {
    String todayIso = Instant.now(fixedClock).toString();
    LocalTime pastTime = LocalTime.now(fixedClock.withZone(ZoneOffset.UTC)).minusHours(1);
    assertTrue(validator.isTodayAndAlreadyStarted(todayIso, pastTime));
  }

  @Test
  void isTodayAndAlreadyStarted_TodayAndTimeEqualNow_ReturnsTrue() {
    String todayIso = Instant.now(fixedClock).toString();
    LocalTime currentTime = LocalTime.now(fixedClock.withZone(ZoneOffset.UTC));
    assertTrue(validator.isTodayAndAlreadyStarted(todayIso, currentTime));
  }

  @Test
  void isTodayAndAlreadyStarted_TodayMidnight_ReturnsTrue() {
    String todayIso =
        LocalDate.now(fixedClock.withZone(ZoneOffset.UTC)).atStartOfDay(ZoneOffset.UTC).toInstant().toString();
    assertTrue(validator.isTodayAndAlreadyStarted(todayIso, LocalTime.MIDNIGHT));
  }

  @Test
  void hasOverlap_NullDeployments_ReturnsFalse() {
    assertFalse(
        validator.hasOverlap(
            null, "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_EmptyDeployments_ReturnsFalse() {
    assertFalse(
        validator.hasOverlap(
            List.of(), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_NullDate_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build();
    assertFalse(
        validator.hasOverlap(List.of(existing), null, LocalTime.of(10, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_NullTargetStart_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build();
    assertFalse(
        validator.hasOverlap(List.of(existing), "2024-01-01T00:00:00Z", null, LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_NullTargetEnd_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(9, 0))
            .endTime(LocalTime.of(10, 0))
            .build();
    assertFalse(
        validator.hasOverlap(List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), null));
  }

  @Test
  void hasOverlap_DifferentDates_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-02T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_ExistingNullDate_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date(null)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_ExistingNullStartTime_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(null)
            .endTime(LocalTime.of(11, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_ExistingNullEndTime_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(null)
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(9, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_NoOverlap_TargetBeforeExisting_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(12, 0))
            .endTime(LocalTime.of(13, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(11, 0)));
  }

  @Test
  void hasOverlap_NoOverlap_TargetAfterExisting_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(12, 0), LocalTime.of(13, 0)));
  }

  @Test
  void hasOverlap_Overlap_TargetStartsDuringExisting_ReturnsTrue() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(13, 0))
            .build();
    assertTrue(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(11, 0), LocalTime.of(14, 0)));
  }

  @Test
  void hasOverlap_Overlap_TargetEndsDuringExisting_ReturnsTrue() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(11, 0))
            .endTime(LocalTime.of(14, 0))
            .build();
    assertTrue(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_Overlap_TargetCompletelyInsideExisting_ReturnsTrue() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(14, 0))
            .build();
    assertTrue(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(11, 0), LocalTime.of(13, 0)));
  }

  @Test
  void hasOverlap_Overlap_ExistingCompletelyInsideTarget_ReturnsTrue() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(11, 0))
            .endTime(LocalTime.of(13, 0))
            .build();
    assertTrue(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(14, 0)));
  }

  @Test
  void hasOverlap_Overlap_ExactSameTime_ReturnsTrue() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(12, 0))
            .build();
    assertTrue(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_Overlap_TargetEndsExactlyWhenExistingStarts_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(12, 0))
            .endTime(LocalTime.of(14, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(10, 0), LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_Overlap_TargetStartsExactlyWhenExistingEnds_ReturnsFalse() {
    Deployment existing =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(12, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing), "2024-01-01T00:00:00Z", LocalTime.of(12, 0), LocalTime.of(14, 0)));
  }

  @Test
  void hasOverlap_MultipleDeployments_OneOverlaps_ReturnsTrue() {
    Deployment existing1 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(9, 0))
            .build();
    Deployment existing2 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(11, 0))
            .endTime(LocalTime.of(13, 0))
            .build();
    Deployment existing3 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(15, 0))
            .endTime(LocalTime.of(16, 0))
            .build();
    assertTrue(
        validator.hasOverlap(
            List.of(existing1, existing2, existing3),
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_MultipleDeployments_NoneOverlap_ReturnsFalse() {
    Deployment existing1 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(9, 0))
            .build();
    Deployment existing2 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(15, 0))
            .endTime(LocalTime.of(16, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing1, existing2),
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_MultipleDeployments_DifferentDates_ReturnsFalse() {
    Deployment existing1 =
        Deployment.builder()
            .date("2024-01-02T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(12, 0))
            .build();
    Deployment existing2 =
        Deployment.builder()
            .date("2024-01-03T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(12, 0))
            .build();
    assertFalse(
        validator.hasOverlap(
            List.of(existing1, existing2),
            "2024-01-01T00:00:00Z",
            LocalTime.of(10, 0),
            LocalTime.of(12, 0)));
  }

  @Test
  void hasOverlap_MultipleDeployments_SomeNullFields_ReturnsFalse() {
    Deployment existing1 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(null)
            .endTime(LocalTime.of(12, 0))
            .build();

    Deployment existing2 =
        Deployment.builder()
            .date("2024-01-01T00:00:00Z")
            .startTime(LocalTime.of(10, 0))
            .endTime(null)
            .build();

    Deployment existing3 =
        Deployment.builder()
            .date(null)
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(12, 0))
            .build();

    assertFalse(
        validator.hasOverlap(
            List.of(existing1, existing2, existing3),
            "2024-01-01T00:00:00Z",
            LocalTime.of(11, 0),
            LocalTime.of(13, 0)));
  }

  @Test
  void isCommitHashValid_ValidHash_ReturnsTrue() throws Exception {
    String validHash = "abc123def456";

    @SuppressWarnings("unchecked")
    HttpResponse<Void> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    assertTrue(validator.isCommitHashValid(validHash));
  }

  @Test
  void isCommitHashValid_InvalidHash_ReturnsFalse() throws Exception {
    String invalidHash = "abcdef123456";

    @SuppressWarnings("unchecked")
    HttpResponse<Void> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(404);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    assertFalse(validator.isCommitHashValid(invalidHash));
  }

  @Test
  void isCommitHashValid_HttpException_ReturnsFalse() throws Exception {
    String hash = "abcdef123456";

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new RuntimeException("Network error"));

    assertFalse(validator.isCommitHashValid(hash));
  }

  @Test
  void isCommitHashValid_NullHash_ReturnsFalse() {
    assertFalse(validator.isCommitHashValid(null));
  }

  @Test
  void isCommitHashValid_EmptyHash_ReturnsFalse() {
    assertFalse(validator.isCommitHashValid(""));
  }
}
