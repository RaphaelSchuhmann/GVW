package com.gvw.gvwbackend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
  private static final Logger log = LoggerFactory.getLogger(SseService.class);

  public SseEmitter createEmitter() {
    SseEmitter emitter = new SseEmitter(3600_000L);

    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError((e) -> emitters.remove(emitter));

    emitters.add(emitter);

    try {
      emitter.send(SseEmitter.event().name("connected").data("success"));
    } catch (Exception ex) {
      emitters.remove(emitter);
    }

    return emitter;
  }

  public void broadcastRefresh(String entityType) {
    List<SseEmitter> deadEmitters = new ArrayList<>();

    emitters.forEach(
        emitter -> {
          try {
            emitter.send(SseEmitter.event().name("refresh").data(entityType));
          } catch (Exception ex) {
            log.debug("Failed to send refresh event to emitter: {}", ex.getMessage());
            deadEmitters.add(emitter);
          }
        });

    emitters.removeAll(deadEmitters);
  }

  @Scheduled(fixedRate = 25000)
  public void sendHeartbeat() {
    if (emitters.isEmpty()) return;
    List<SseEmitter> deadEmitters = new ArrayList<>();

    log.debug("Sending heartbeat to {} active clients", emitters.size());

    emitters.forEach(
        emitter -> {
          try {
            emitter.send(SseEmitter.event().comment("heartbeat"));
          } catch (Exception ex) {
            deadEmitters.add(emitter);
          }
        });

    emitters.removeAll(deadEmitters);
  }
}
