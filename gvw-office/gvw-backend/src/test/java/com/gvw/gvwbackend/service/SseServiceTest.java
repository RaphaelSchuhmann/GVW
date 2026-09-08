package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  void sendHeartbeat_WithEmitters_Success() throws Exception {
    SseEmitter emitter = mock(SseEmitter.class);

    Field emittersField = SseService.class.getDeclaredField("emitters");
    emittersField.setAccessible(true);

    @SuppressWarnings("unchecked")
    List<SseEmitter> emitters = (List<SseEmitter>) emittersField.get(sseService);

    emitters.add(emitter);

    sseService.sendHeartbeat();

    ArgumentCaptor<SseEmitter.SseEventBuilder> captor =
        ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);

    verify(emitter).send(captor.capture());

    assertNotNull(captor.getValue());
  }

  @Test
  void sendHeartbeat_WithoutEmitters_DoesNothing() {
    assertDoesNotThrow(() -> sseService.sendHeartbeat());
  }
}
