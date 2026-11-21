package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.exception.ServiceUnavailableException;
import com.gvw.gvwbackend.state.MaintenanceState;
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
  private final MaintenanceState maintenanceState;

  /**
   * Creates the SSE service with the shared maintenance state used to reject new connections during
   * deployments.
   *
   * @param state the shared maintenance state
   */
  public SseService(MaintenanceState state) {
    this.maintenanceState = state;
  }

  /**
   * Creates and registers a new SSE connection.
   *
   * <p>Creates an emitter with a one-hour timeout, registers lifecycle callbacks to remove
   * disconnected clients, and sends an initial connection event.
   *
   * @return newly created and registered SSE emitter
   */
  public SseEmitter createEmitter() {
    if (maintenanceState.getActive()) {
      // Hardcoded status code is fine here as it is a general infra error
      throw new ServiceUnavailableException("0000503");
    }

    SseEmitter emitter = new SseEmitter(3600_000L);

    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError(
        e -> {
          log.debug("SSE client disconnected: {}", e.getMessage());
          emitters.remove(emitter);
        });

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
   * Generic internal broadcast runner that handles SSE delivery, dead emitter cleanup, and
   * exceptions during iteration.
   *
   * @param eventName name of the SSE event (e.g., "refresh", "kill")
   * @param data payload data to attach, or {@code null} if no payload is required
   */
  private void broadcast(String eventName, Object data) {
    List<SseEmitter> deadEmitters = new ArrayList<>();

    emitters.forEach(
        emitter -> {
          try {
            SseEmitter.SseEventBuilder event = SseEmitter.event().name(eventName);
            if (data != null) {
              event.data(data);
            }
            emitter.send(event);
          } catch (Exception ex) {
            log.debug("Failed to send {} event to emitter: {}", eventName, ex.getMessage());
            deadEmitters.add(emitter);
          }
        });

    emitters.removeAll(deadEmitters);
  }

  /**
   * Safely executes a broadcast, swallowing unchecked exceptions to prevent SSE failures from
   * interrupting the caller flow.
   */
  private void safeBroadcast(String eventName, Object data) {
    try {
      broadcast(eventName, data);
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast {} event", eventName, ex);
    }
  }

  /**
   * Triggers a non-blocking refresh broadcast for the specified entity type.
   *
   * @param entityType type of entity that changed and requires client-side refreshing
   */
  public void sendRefresh(String entityType) {
    safeBroadcast("refresh", entityType);
  }

  /**
   * Broadcasts a kill event to all connected clients without a payload and sets isKilled flag to
   * true.
   */
  public void sendKillSignal() {
    safeBroadcast("kill", null);
    // Complete (not just remove) so the underlying HTTP connections close before shutdown
    emitters.forEach(SseEmitter::complete);
    emitters.clear();
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
            log.debug("SSE client disconnected: {}", ex.getMessage());
            deadEmitters.add(emitter);
          }
        });

    emitters.removeAll(deadEmitters);
  }
}
