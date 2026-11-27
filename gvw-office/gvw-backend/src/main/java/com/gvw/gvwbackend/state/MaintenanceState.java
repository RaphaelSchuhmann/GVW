package com.gvw.gvwbackend.state;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Shared application state tracking whether the system is currently in maintenance mode.
 *
 * <p>While active, incoming requests are rejected and new SSE connections are refused.
 */
@Component
public class MaintenanceState {
  private final AtomicBoolean isInMaintenance = new AtomicBoolean(false);

  /**
   * Sets the maintenance mode flag.
   *
   * @param value {@code true} to enable maintenance mode, {@code false} to disable it.
   */
  public void setActive(boolean value) {
    isInMaintenance.set(value);
  }

  /**
   * Returns whether maintenance mode is currently active.
   *
   * @return {@code true} if the system is in maintenance mode.
   */
  public boolean getActive() {
    return isInMaintenance.get();
  }
}
