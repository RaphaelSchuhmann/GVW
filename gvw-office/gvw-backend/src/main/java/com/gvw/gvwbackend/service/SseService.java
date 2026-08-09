package com.gvw.gvwbackend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service responsible for managing Server-Sent Events (SSE) connections.
 *
 * <p>Maintains active client connections, sends real-time update events, and removes disconnected
 * clients automatically. Also sends periodic heartbeat events to keep SSE connections alive.
 */
@Service
public class SseService {
  /** Thread-safe collection of currently connected SSE clients. */
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  private static final Logger log = LoggerFactory.getLogger(SseService.class);

  /**
   * Creates and registers a new SSE connection.
   *
   * <p>Creates an emitter with a one-hour timeout, registers lifecycle callbacks to remove
   * disconnected clients, and sends an initial connection event.
   *
   * @return newly created and registered SSE emitter
   */
  public SseEmitter createEmitter() {
    SseEmitter emitter = new SseEmitter(3600_000L);

    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError((e) -> emitters.remove(emitter));

    emitters.add(emitter);

    try {
      emitter.send(SseEmitter.event().name("connected").data("success"));
    } catch (Exception ex) {
      log.error("Failed to send initial SSE connection event", ex);
      emitters.remove(emitter);
    }

    return emitter;
  }

  /**
   * Broadcasts a refresh event to all connected clients.
   *
   * <p>Clients receive an event named {@code refresh} containing the affected entity type. Emitters
   * that can no longer receive events are removed.
   *
   * @param entityType type of entity that has changed and requires refreshing
   */
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

  /**
   * Sends heartbeat events to all active SSE connections.
   *
   * <p>Heartbeat comments keep long-lived HTTP connections alive and allow inactive connections to
   * be detected and removed.
   *
   * <p>This method runs automatically every 25 seconds.
   */
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
