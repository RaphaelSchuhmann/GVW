package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.dto.request.AddEventRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateEventRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateEventStatusRequestDTO;
import com.gvw.gvwbackend.dto.response.EventsResponseDTO;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.ErrorResource;
import com.gvw.gvwbackend.exception.handler.ErrorContext;
import com.gvw.gvwbackend.service.EventService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
public class EventController {
  private final EventService eventService;

  public EventController(EventService eventService) {
    this.eventService = eventService;
  }

  @GetMapping("/all")
  public EventsResponseDTO allEvents() {
    return eventService.allEvents();
  }

  @PostMapping("/add")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.EVENTS,
      action = ErrorAction.CREATE,
      resource = ErrorResource.NONE)
  public void addEvent(@Valid @RequestBody AddEventRequestDTO request) {
    eventService.addEvent(request);
  }

  @GetMapping("/check/{id}")
  @ResponseStatus(HttpStatus.OK)
  public void checkEvent(@PathVariable String id) {
    eventService.checkEvent(id);
  }

  @DeleteMapping("/delete/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  public void deleteEvent(@PathVariable String id) {
    eventService.deleteEvent(id);
  }

  @PatchMapping("/update/status/{id}")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.EVENTS,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateEventStatus(
      @PathVariable String id, @Valid @RequestBody UpdateEventStatusRequestDTO request) {
    String rev = eventService.updateEventStatus(id, request.rev());
    return Map.of("rev", rev);
  }

  @PatchMapping("/update")
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('ADMIN', 'BOARD_MEMBER')")
  @ErrorContext(
      domain = ErrorDomain.EVENTS,
      action = ErrorAction.UPDATE,
      resource = ErrorResource.NONE)
  public Map<String, Object> updateEvent(@Valid @RequestBody UpdateEventRequestDTO request) {
    String rev = eventService.updateEvent(request);
    return Map.of("rev", rev);
  }
}
