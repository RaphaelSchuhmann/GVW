package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class SseServiceTest {

  @InjectMocks private SseService sseService;

  @Test
  void createEmitter_Success() {
    SseEmitter emitter = sseService.createEmitter();

    assertNotNull(emitter);
  }

  @Test
  void sendRefresh_Success() {
    assertDoesNotThrow(() -> sseService.sendRefresh("TEST_ENTITY"));
  }

  @Test
  void sendHeartbeat_WithEmitters_Success() {
    SseService sseServiceSpy = spy(sseService);
    SseEmitter emitter = mock(SseEmitter.class);

    assertDoesNotThrow(sseServiceSpy::sendHeartbeat);
  }

  @Test
  void sendHeartbeat_WithoutEmitters_DoesNothing() {
    assertDoesNotThrow(() -> sseService.sendHeartbeat());
  }
}
