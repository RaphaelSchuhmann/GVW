package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.model.Deployment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DeploymentValidator {
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private static final Logger log = LoggerFactory.getLogger(DeploymentValidator.class);

  public boolean isTodayAndAlreadyStarted(String targetIsoDate, LocalTime targetStart) {
    if (targetIsoDate == null || targetStart == null) {
      return false;
    }

    LocalDate targetDate = Instant.parse(targetIsoDate).atZone(ZoneOffset.UTC).toLocalDate();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    if (!targetDate.equals(today)) {
      return false; // Not today (either past date or future date)
    }

    LocalTime currentTime = LocalTime.now(ZoneOffset.UTC);

    boolean result = !currentTime.isBefore(targetStart);
    if (result) {
      log.debug("A past date or startTime was passed");
    }

    return result;
  }

  public boolean hasOverlap(
      List<Deployment> deployments,
      String targetIsoDate,
      LocalTime targetStart,
      LocalTime targetEnd) {
    if (deployments == null
        || deployments.isEmpty()
        || targetIsoDate == null
        || targetStart == null
        || targetEnd == null) {
      return false;
    }

    LocalDate targetDate = extractLocalDate(targetIsoDate);

    boolean result =
        deployments.stream()
            .anyMatch(
                existing -> {
                  if (existing.getDate() == null
                      || existing.getStartTime() == null
                      || existing.getEndTime() == null) {
                    return false;
                  }

                  LocalDate existingDate = extractLocalDate(existing.getDate());

                  if (!targetDate.equals(existingDate)) {
                    return false;
                  }

                  return targetStart.isBefore(existing.getEndTime())
                      && targetEnd.isAfter(existing.getStartTime());
                });

    if (result) {
      log.debug("A time overlap was detected");
    }

    return result;
  }

  public boolean isCommitHashValid(String hash) {
    log.debug("Validating commit hash {}", hash);
    String url = "https://api.github.com/repos/raphaelschuhmann/gvw/commits/" + hash;
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Java-Commit-Validator")
            .header("Accept", "application/vnd.github+json")
            .method("HEAD", HttpRequest.BodyPublishers.noBody())
            .build();

    try {
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() == 200;
    } catch (Exception e) {
      log.debug("Invalid commit hash passed");
      return false;
    }
  }

  private LocalDate extractLocalDate(String isoTimestamp) {
    return Instant.parse(isoTimestamp).atZone(ZoneOffset.UTC).toLocalDate();
  }
}
