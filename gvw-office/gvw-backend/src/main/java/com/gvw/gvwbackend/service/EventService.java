package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddEventRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateEventRequestDTO;
import com.gvw.gvwbackend.dto.response.EventResponseDTO;
import com.gvw.gvwbackend.dto.response.EventsResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.mapper.EventMapper;
import com.gvw.gvwbackend.model.Event;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventService {
  private final ObjectMapper mapper = new ObjectMapper();
  private final DbService dbService;
  private final EventMapper eventMapper;
  private final SseService sseService;
  private static final Logger log = LoggerFactory.getLogger(EventService.class);

  public EventService(DbService dbService, EventMapper eventMapper, SseService sseService) {
    this.dbService = dbService;
    this.eventMapper = eventMapper;
    this.sseService = sseService;
  }

  public EventsResponseDTO allEvents() {
    List<Map<String, Object>> eventsRaw = dbService.findAll("events");
    boolean changed = false;

    List<Event> events =
        eventsRaw.stream().map(map -> mapper.convertValue(map, Event.class)).toList();

    if (events.isEmpty()) {
      return new EventsResponseDTO(List.of());
    }

    for (Event event : events) {
      if (event.getDate() == null || event.getStatus() == null || event.getMode() == null) {
        continue;
      }

      Instant eventDate;
      try {
        eventDate = Instant.parse(event.getDate());
      } catch (Exception ex) {
        log.error("Error converting event date string: {}", event.getDate(), ex);
        throw new RuntimeException(
            String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.READ_ALL, 500)), ex);
      }

      if (eventDate.isBefore(Instant.now())
          && event.getStatus().equals("upcoming")
          && event.getMode().equalsIgnoreCase("single")) {
        event.setStatus("finished");
        Map<String, Object> resp = dbService.update("events", event.getId(), event);

        if (resp == null || !resp.containsKey("rev")) {
          event.setStatus("upcoming");
          continue;
        }

        event.setRev((String) resp.get("rev"));
        changed = true;
      }
    }

    if (changed) {
      try {
        sseService.broadcastRefresh("EVENTS");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast EVENTS refresh: ", ex);
      }
    }

    List<EventResponseDTO> responseEvents =
        events.stream()
            .map(
                m ->
                    new EventResponseDTO(
                        m.getId(),
                        m.getRev(),
                        m.getTitle(),
                        m.getType(),
                        m.getDate(),
                        m.getTime(),
                        m.getLocation(),
                        m.getDescription(),
                        m.getMode(),
                        m.getStatus(),
                        m.getRecurrence()))
            .toList();

    return new EventsResponseDTO(responseEvents);
  }

  public void checkEvent(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.CHECK, 400)));
    }

    Event event = dbService.findById("events", id, Event.class);
    if (event == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.CHECK, 404)));
    }
  }

  public void addEvent(AddEventRequestDTO request) {
    if (!validateRecurrence(request.mode(), request.recurrence())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.CREATE, 400)));
    }

    Event event = createEventFromRequest(request);

    dbService.insert("events", event);

    try {
      sseService.broadcastRefresh("EVENTS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast EVENTS refresh: ", ex);
    }
  }

  public void deleteEvent(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.DELETE, 400)));
    }

    Event event = dbService.findById("events", id, Event.class);
    if (event == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.DELETE, 404)));
    }

    dbService.delete("events", event.getId(), event.getRev());

    try {
      sseService.broadcastRefresh("EVENTS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast EVENTS refresh: ", ex);
    }
  }

  public String updateEventStatus(String id, String _rev) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.UPDATE, 400)));
    }

    Event event = dbService.findById("events", id, Event.class);
    if (event == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.UPDATE, 404)));
    }

    event.setRev(_rev);

    if ("upcoming".equals(event.getStatus())) {
      event.setStatus("finished");
    } else {
      event.setStatus("upcoming");
    }

    Map<String, Object> resp = dbService.update("events", event.getId(), event);

    try {
      sseService.broadcastRefresh("EVENTS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast EVENTS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.UPDATE, 500)));
  }

  public String updateEvent(UpdateEventRequestDTO request) {
    Event event = dbService.findById("events", request.id(), Event.class);

    if (event == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.UPDATE, 404)));
    }

    eventMapper.updateEventFromDto(request, event);

    event.setRev(request.rev());

    Map<String, Object> resp = dbService.update("events", event.getId(), event);

    try {
      sseService.broadcastRefresh("EVENTS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast EVENTS refresh: ", ex);
    }

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.EVENTS.createCode(ErrorAction.UPDATE, 500)));
  }

  private boolean validateRecurrence(String mode, Event.Recurrence recurrence) {
    if (mode.equalsIgnoreCase("single") || mode.equalsIgnoreCase("weekly")) return true;

    if (recurrence == null) return false;

    if (mode.equalsIgnoreCase("monthly")
        && recurrence.getMonthlyKind() != null
        && !recurrence.getMonthlyKind().isBlank()) {
      return switch (recurrence.getMonthlyKind()) {
        case "weekday" ->
            recurrence.getWeekDay() != null
                && !recurrence.getWeekDay().isBlank()
                && recurrence.getOrdinal() != null
                && !recurrence.getOrdinal().isBlank();
        case "date" -> recurrence.getDayOfMonth() != null && !recurrence.getDayOfMonth().isBlank();
        default -> false;
      };
    }

    return false;
  }

  private Event createEventFromRequest(AddEventRequestDTO request) {
    Event event = new Event();
    event.setTitle(request.title());
    event.setType(request.type());
    event.setDate(request.date());
    event.setTime(request.time());
    event.setLocation(request.location());
    event.setDescription(request.description());
    event.setMode(request.mode());
    event.setStatus(request.status());
    event.setRecurrence(request.recurrence());

    return event;
  }
}
