package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.model.helper.ErrorMessageType;
import com.gvw.gvwbackend.model.helper.StatusMessage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ErrorCodeMapper {
  private static final Map<Integer, StatusMessage> ERROR_MESSAGES = new LinkedHashMap<>();

  static {
    // No Error & General
    ERROR_MESSAGES.put(
        0,
        new StatusMessage(
            "Aktion erfolgreich",
            "Die zuletzt gestartete Aktion wurde erfolgreich ausgeführt.",
            ErrorMessageType.SUCCESS));
    ERROR_MESSAGES.put(
        1,
        new StatusMessage(
            "Unbekannter Fehler",
            "Ein unbekannter fehler ist aufgetreten.",
            ErrorMessageType.ERROR));

    // BUILD Codes
    ERROR_MESSAGES.put(
        1 << 1,
        new StatusMessage(
            "Build erfolgreich",
            "Der build für dieses Deployment wurde erfolgreich abgeschlossen.",
            ErrorMessageType.SUCCESS));
    ERROR_MESSAGES.put(
        1 << 2,
        new StatusMessage(
            "Build fehlgeschlagen",
            "Der build für dieses Deployment ist fehlgeschlagen.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 3,
        new StatusMessage(
            "Ungültiger Commit",
            "Der übergebene Commit ist nicht gültig oder existiert nicht.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 4,
        new StatusMessage(
            "Tests fehlgeschlagen",
            "Die Tests von Frontend und oder Backend sind fehlgeschlagen.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 5,
        new StatusMessage(
            "Build fehlgeschlagen",
            "Fehler beim extrahieren der Build-Artifacts.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 6,
        new StatusMessage(
            "Build fehlgeschlagen",
            "Schema konnte nicht generiert werden.",
            ErrorMessageType.ERROR));

    // Migration Codes
    ERROR_MESSAGES.put(
        1 << 7,
        new StatusMessage(
            "Migration Dry-Run erfolgreich",
            "Der Migration Dry-Run konnte erfolgreich ausgeführt werden.",
            ErrorMessageType.SUCCESS));
    ERROR_MESSAGES.put(
        1 << 8,
        new StatusMessage(
            "Migration Dry-Run fehlgeschlagen",
            "Das Schema war nicht vorhanden oder fehlerhaft.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 9,
        new StatusMessage(
            "Datenbank nicht gefunden",
            "Die angegebene Datenbank wurde nicht in der Konfiguration gefunden.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 10,
        new StatusMessage(
            "Ungültige betroffene Datenbanken",
            "Die Liste der betroffenen Datenbanken ist ungültig.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 11,
        new StatusMessage(
            "Ungültige Migration gefunden",
            "Eine ungültige Migration wurde im Deployment gefunden.",
            ErrorMessageType.ERROR));

    // Deploy Codes
    ERROR_MESSAGES.put(
        1 << 12,
        new StatusMessage(
            "Ungültige Deployment-ID",
            "Die angegebene Deployment-ID ist ungültig oder existiert nicht.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 13,
        new StatusMessage(
            "Kein Build gefunden",
            "Für dieses Deployment wurde kein Build gefunden und ein neuer Build ist fehlgeschlagen.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 14,
        new StatusMessage(
            "Nginx Wartungsmodus fehlgeschlagen",
            "Der Wechsel in den Nginx Wartungsmodus ist fehlgeschlagen.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 15,
        new StatusMessage(
            "Backend-Stopp fehlgeschlagen",
            "Das Backend konnte nicht gestoppt werden.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 16,
        new StatusMessage(
            "Backend-Start fehlgeschlagen",
            "Das Backend konnte nicht gestartet werden.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 17,
        new StatusMessage(
            "Artefakt-Installation fehlgeschlagen",
            "Die Installation der Build-Artefakte ist fehlgeschlagen.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 18,
        new StatusMessage(
            "Migrationen fehlgeschlagen",
            "Die Datenbankmigrationen während des Deployments sind fehlgeschlagen.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 19,
        new StatusMessage(
            "Anwendung unhealthy",
            "Die Anwendung ist nach dem Deployment nicht healthy.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 20,
        new StatusMessage(
            "Wiederherstellung erforderlich",
            "Das Deployment erfordert eine manuelle Wiederherstellung.",
            ErrorMessageType.ERROR));
    ERROR_MESSAGES.put(
        1 << 21,
        new StatusMessage(
            "Deployment läuft bereits",
            "Deployment konnte nicht gestartet werden da bereits ein anderes Deployment läuft.",
            ErrorMessageType.WARNING)); // -> technically not possible
    ERROR_MESSAGES.put(
        1 << 22,
        new StatusMessage(
            "Deployment erfolgreich",
            "Das Deployment wurde erfolgreich durch geführt.",
            ErrorMessageType.SUCCESS)); // -> technically not possible
  }

  /**
   * Resolves an integer error code to its message. 1. Checks for an exact match in the lookup
   * table. 2. If not found, decomposes the bitmask into individual active flags. 3. Falls back to a
   * default message if no bits match.
   */
  public static List<StatusMessage> getMessages(int code) {
    if (ERROR_MESSAGES.containsKey(code)) {
      return List.of(ERROR_MESSAGES.get(code));
    }

    List<StatusMessage> matchedMessages = new ArrayList<>();
    for (Map.Entry<Integer, StatusMessage> entry : ERROR_MESSAGES.entrySet()) {
      int flag = entry.getKey();

      // Skip 0 when checking flags
      if (flag != 0 && (code & flag) == flag) {
        matchedMessages.add(entry.getValue());
      }
    }

    if (!matchedMessages.isEmpty()) {
      return matchedMessages;
    }

    return List.of(
        new StatusMessage(
            "Unbekannter Fehler",
            "Ein unbekannter fehler ist aufgetreten mit code: " + code,
            ErrorMessageType.ERROR));
  }
}
