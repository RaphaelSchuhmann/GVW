package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddEventRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateEventRequestDTO;
import com.gvw.gvwbackend.dto.response.EventResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.mapper.EventMapper;
import com.gvw.gvwbackend.model.Event;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock private DbService dbService;

  @Mock private EventMapper eventMapper;

  @Mock private SseService sseService;

  @InjectMocks private EventService eventService;

  private Event event;

  @BeforeEach
  void setUp() {
    event = new Event();
    event.setId("event-1");
    event.setRev("1-abc");
    event.setTitle("Test Event");
    event.setType("rehearsal");
    event.setDate(Instant.now().plusSeconds(3600).toString());
    event.setTime(LocalTime.of(18, 0));
    event.setLocation("Hall");
    event.setDescription("Test description");
    event.setMode("single");
    event.setStatus("upcoming");
  }

  @Test
  void allEvents_Success() {
    when(dbService.findAll("events", Event.class)).thenReturn(List.of(event));

    List<EventResponseDTO> result = eventService.allEvents();

    assertEquals(1, result.size());
    assertEquals("event-1", result.getFirst().id());
    assertEquals("Test Event", result.getFirst().title());
  }

  @Test
  void allEvents_Empty_ReturnsEmptyList() {
    when(dbService.findAll("events", Event.class)).thenReturn(List.of());

    List<EventResponseDTO> result = eventService.allEvents();

    assertTrue(result.isEmpty());
  }

  @Test
  void allEvents_ExpiredSingleEvent_UpdatesStatus() {
    event.setDate(Instant.now().minusSeconds(3600).toString());
    when(dbService.findAll("events", Event.class)).thenReturn(List.of(event));
    when(dbService.update("events", "event-1", event)).thenReturn("2-def");

    List<EventResponseDTO> result = eventService.allEvents();

    assertEquals("finished", event.getStatus());
    verify(sseService).sendRefresh("EVENTS");
  }

  @Test
  void checkEvent_Success() {
    when(dbService.findById("events", "event-1", Event.class)).thenReturn(event);

    assertDoesNotThrow(() -> eventService.checkEvent("event-1"));
  }

  @Test
  void checkEvent_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> eventService.checkEvent(null));
  }

  @Test
  void checkEvent_BlankId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> eventService.checkEvent(""));
  }

  @Test
  void checkEvent_NotFound_ThrowsNotFound() {
    when(dbService.findById("events", "non-existent", Event.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> eventService.checkEvent("non-existent"));
  }

  @Test
  void addEvent_Success() {
    AddEventRequestDTO request =
        new AddEventRequestDTO(
            "New Event",
            "rehearsal",
            Instant.now().plusSeconds(3600).toString(),
            LocalTime.of(18, 0),
            "Hall",
            "Description",
            "upcoming",
            "single",
            null);

    assertDoesNotThrow(() -> eventService.addEvent(request));
    verify(dbService).insert(eq("events"), any(Event.class));
    verify(sseService).sendRefresh("EVENTS");
  }

  @Test
  void addEvent_InvalidRecurrence_ThrowsBadRequest() {
    Event.Recurrence recurrence = new Event.Recurrence();
    recurrence.setMonthlyKind("weekday");
    recurrence.setWeekDay(null);
    AddEventRequestDTO request =
        new AddEventRequestDTO(
            "New Event",
            "rehearsal",
            Instant.now().plusSeconds(3600).toString(),
            LocalTime.of(18, 0),
            "Hall",
            "Description",
            "upcoming",
            "monthly",
            recurrence);

    assertThrows(BadRequestException.class, () -> eventService.addEvent(request));
  }

  @Test
  void deleteEvent_Success() {
    when(dbService.findById("events", "event-1", Event.class)).thenReturn(event);

    eventService.deleteEvent("event-1");

    verify(dbService).delete("events", "event-1", "1-abc");
    verify(sseService).sendRefresh("EVENTS");
  }

  @Test
  void deleteEvent_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> eventService.deleteEvent(null));
  }

  @Test
  void deleteEvent_NotFound_ThrowsNotFound() {
    when(dbService.findById("events", "non-existent", Event.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> eventService.deleteEvent("non-existent"));
  }

  @Test
  void updateEventStatus_Success() {
    when(dbService.findById("events", "event-1", Event.class)).thenReturn(event);
    when(dbService.update("events", "event-1", event)).thenReturn("2-def");

    String result = eventService.updateEventStatus("event-1", "1-abc");

    assertEquals("2-def", result);
    assertEquals("finished", event.getStatus());
    verify(sseService).sendRefresh("EVENTS");
  }

  @Test
  void updateEventStatus_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> eventService.updateEventStatus(null, "1-abc"));
  }

  @Test
  void updateEventStatus_NotFound_ThrowsNotFound() {
    when(dbService.findById("events", "non-existent", Event.class)).thenReturn(null);

    assertThrows(
        NotFoundException.class, () -> eventService.updateEventStatus("non-existent", "1-abc"));
  }

  @Test
  void updateEvent_Success() {
    UpdateEventRequestDTO request =
        new UpdateEventRequestDTO(
            "event-1",
            "Updated Title",
            "rehearsal",
            Instant.now().plusSeconds(3600).toString(),
            LocalTime.of(18, 0),
            "Hall",
            "Description",
            "upcoming",
            "single",
            null,
            "1-abc");
    when(dbService.findById("events", "event-1", Event.class)).thenReturn(event);
    when(dbService.update("events", "event-1", event)).thenReturn("2-def");

    String result = eventService.updateEvent(request);

    assertEquals("2-def", result);
    verify(sseService).sendRefresh("EVENTS");
  }

  @Test
  void updateEvent_NotFound_ThrowsNotFound() {
    UpdateEventRequestDTO request =
        new UpdateEventRequestDTO(
            "non-existent",
            "Title",
            "rehearsal",
            Instant.now().plusSeconds(3600).toString(),
            LocalTime.of(18, 0),
            "Hall",
            "Description",
            "upcoming",
            "single",
            null,
            "1-abc");
    when(dbService.findById("events", "non-existent", Event.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> eventService.updateEvent(request));
  }
}
