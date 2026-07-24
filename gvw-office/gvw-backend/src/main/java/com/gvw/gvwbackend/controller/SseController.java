package com.gvw.gvwbackend.controller;

import com.gvw.gvwbackend.service.SseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sync")
public class SseController {
  private final SseService sseService;

  public SseController(SseService sseService) {
    this.sseService = sseService;
  }

  @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe() {
    return sseService.createEmitter();
  }
}
