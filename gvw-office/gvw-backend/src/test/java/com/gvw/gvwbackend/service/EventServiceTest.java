package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddEventRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateEventRequestDTO;
import com.gvw.gvwbackend.dto.response.EventsResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.mapper.EventMapper;
import com.gvw.gvwbackend.model.Event;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventServiceTest {
  @Mock private DbService dbService;
  @Mock private EventMapper eventMapper;
  @Mock private SseService sseService;

  @InjectMocks private EventService eventService;

  private Event validEvent;

  @BeforeEach
  void setUp() {
    validEvent = new Event();
    validEvent.setId("event-123");
    validEvent.setRev("1-rev");
    validEvent.setTitle("Practice");
    validEvent.setStatus("upcoming");
    validEvent.setMode("single");
    validEvent.setDate("2028-01-01T00:00:00.000Z");
  }

  @Test
  void testAllEventsShouldReturnEmptyListWhenNoEventsFound() {
    when(dbService.findAll("events")).thenReturn(List.of());

    EventsResponseDTO response = eventService.allEvents();

    assertTrue(response.data().isEmpty());
    verify(dbService, never()).update(anyString(), any(), any());
  }

  @Test
  void testAllEventsShouldAutoFinishPastSingleEvents() {
    Event pastEvent = new Event();
    pastEvent.setId("past-id");
    pastEvent.setDate("2025-01-01T00:00:00.000Z");
    pastEvent.setStatus("upcoming");
    pastEvent.setMode("Single");

    Map<String, Object> eventMap =
        Map.of(
            "id", "past-id",
            "date", "2025-01-01T00:00:00.000Z",
            "status", "upcoming",
            "mode", "Single");

    when(dbService.findAll("events")).thenReturn(List.of(eventMap));
    when(dbService.update(eq("events"), any(), any(Event.class)))
        .thenReturn(Map.of("rev", "rev-3"));

    EventsResponseDTO response = eventService.allEvents();

    assertEquals("finished", response.data().getFirst().status());
  }

  @Test
  void testAddEventShouldSuccessForSingleMode() {
    AddEventRequestDTO request =
        new AddEventRequestDTO(
            "Title",
            "practice",
            "2026-04-02T00:00:00.000Z",
            LocalTime.of(18, 0),
            "Loc",
            "Desc",
            "upcoming",
            "single",
            null);

    assertDoesNotThrow(() -> eventService.addEvent(request));
    verify(dbService).insert(eq("events"), any(Event.class));
  }

  @Test
  void testAddEventShouldThrowBadRequestForInvalidMonthlyRecurrence() {
    AddEventRequestDTO request =
        new AddEventRequestDTO(
            "Title",
            "practice",
            "2026-04-02T00:00:00.000Z",
            LocalTime.of(18, 0),
            "Loc",
            "Desc",
            "upcoming",
            "monthly",
            null);

    assertThrows(BadRequestException.class, () -> eventService.addEvent(request));
    verify(dbService, never()).insert(any(), any());
  }

  @Test
  void testAddEventShouldSuccessForValidMonthlyDateRecurrence() {
    Event.Recurrence recurrence = new Event.Recurrence("date", "15", null, null);
    AddEventRequestDTO request =
        new AddEventRequestDTO(
            "Title",
            "practice",
            "2026-04-02T00:00:00.000Z",
            LocalTime.of(18, 0),
            "Loc",
            "Desc",
            "upcoming",
            "monthly",
            recurrence);

    assertDoesNotThrow(() -> eventService.addEvent(request));
    verify(dbService).insert(eq("events"), any(Event.class));
  }

  @Test
  void testDeleteEventShouldThrowNotFoundWhenIdDoesNotExist() {
    when(dbService.findById("events", "non-existent", Event.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> eventService.deleteEvent("non-existent"));
  }

  @Test
  void testDeleteEventShouldCallDbDeleteWhenFound() {
    when(dbService.findById("events", "event-123", Event.class)).thenReturn(validEvent);

    eventService.deleteEvent("event-123");

    verify(dbService).delete("events", "event-123", "1-rev");
  }

  @Test
  void testUpdateEventStatusShouldToggleFromUpcomingToFinished() {
    when(dbService.findById("events", "event-123", Event.class)).thenReturn(validEvent);
    when(dbService.update(eq("events"), eq("event-123"), any(Event.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    String rev = eventService.updateEventStatus("event-123", "1-rev");

    assertNotNull(rev);
    assertFalse(rev.isBlank());

    assertEquals("finished", validEvent.getStatus());
    verify(dbService).update("events", validEvent.getId(), validEvent);
  }

  @Test
  void testUpdateEventStatusShouldToggleFromFinishedToUpcoming() {
    validEvent.setStatus("finished");
    when(dbService.findById("events", "event-123", Event.class)).thenReturn(validEvent);
    when(dbService.update(eq("events"), eq("event-123"), any(Event.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    String rev = eventService.updateEventStatus("event-123", "1-rev");

    assertNotNull(rev);
    assertFalse(rev.isBlank());

    assertEquals("upcoming", validEvent.getStatus());
    verify(dbService).update("events", validEvent.getId(), validEvent);
  }

  @Test
  void testUpdateEventShouldFetchApplyMappingAndSave() {
    UpdateEventRequestDTO updateDto =
        new UpdateEventRequestDTO(
            "event-123",
            "New Title",
            "practice",
            "5.5.2026",
            LocalTime.of(19, 0),
            "New Loc",
            "New Desc",
            "upcoming",
            "single",
            null,
            "1-rev");
    when(dbService.findById("events", "event-123", Event.class)).thenReturn(validEvent);
    when(dbService.update(eq("events"), eq("event-123"), any(Event.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    String rev = eventService.updateEvent(updateDto);

    assertNotNull(rev);
    assertFalse(rev.isBlank());

    verify(dbService).findById("events", "event-123", Event.class);
    verify(eventMapper).updateEventFromDto(updateDto, validEvent);
    verify(dbService).update("events", validEvent.getId(), validEvent);
  }

  @Test
  void testUpdateEventShouldThrowNotFoundWhenEventMissing() {
    UpdateEventRequestDTO updateDto =
        new UpdateEventRequestDTO(
            "missing-id",
            "Title",
            "practice",
            "5.5.2026",
            LocalTime.of(19, 0),
            "Loc",
            "Desc",
            "upcoming",
            "single",
            null,
            "1-rev");
    when(dbService.findById("events", "missing-id", Event.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> eventService.updateEvent(updateDto));

    verify(eventMapper, never()).updateEventFromDto(any(), any());
    verify(dbService, never()).update(anyString(), any(), any());
  }
}
