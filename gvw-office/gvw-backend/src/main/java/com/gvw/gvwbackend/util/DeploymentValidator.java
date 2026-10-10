package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.model.Deployment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Utility component responsible for validating deployment timing rules, checking for schedule
 * overlaps, and verifying GitHub commit hashes via the GitHub REST API.
 */
@Component
public class DeploymentValidator {
  private final HttpClient httpClient;
  private final Clock clock;
  private static final Logger log = LoggerFactory.getLogger(DeploymentValidator.class);

  public DeploymentValidator(HttpClient httpClient) {
    this(httpClient, Clock.systemUTC());
  }

  public DeploymentValidator(HttpClient httpClient, Clock clock) {
    this.httpClient = httpClient;
    this.clock = clock;
  }

  /**
   * Checks whether the target date is today in UTC and the specified start time has already passed.
   *
   * @param targetIsoDate The target date string in ISO-8601 format.
   * @param targetStart The target start time.
   * @return True if the date is today and the current time is at or past the target start time.
   */
  public boolean isTodayAndAlreadyStarted(String targetIsoDate, LocalTime targetStart) {
    if (targetIsoDate == null || targetStart == null) {
      return false;
    }

    LocalDate targetDate = Instant.parse(targetIsoDate).atZone(ZoneOffset.UTC).toLocalDate();
    LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));

    if (targetDate.isBefore(today)) {
      return true;
    }

    if (targetDate.isAfter(today)) {
      return false;
    }

    LocalTime currentTime = LocalTime.now(clock.withZone(ZoneOffset.UTC));

    boolean result = !currentTime.isBefore(targetStart);
    if (result) {
      log.debug("A past date or startTime was passed");
    }

    return result;
  }

  /**
   * Checks whether a proposed deployment time slot overlaps with any existing deployments on the
   * same date.
   *
   * @param deployments The list of existing deployments to check against.
   * @param targetIsoDate The target date string in ISO-8601 format.
   * @param targetStart The proposed start time.
   * @param targetEnd The proposed end time.
   * @return True if a time overlap is detected with an existing deployment, false otherwise.
   */
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

  /**
   * Validates if a given commit hash exists on the GitHub repository by issuing a HEAD request.
   *
   * @param hash The commit hash string to validate.
   * @return True if the commit exists (HTTP 200), false otherwise.
   */
  public boolean isCommitHashValid(String hash) {
    if (hash == null || !hash.matches("^[0-9a-fA-F]{7,40}$")) {
      return false;
    }
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

  /**
   * Helper method to parse an ISO-8601 timestamp string into a UTC {@link LocalDate}.
   *
   * @param isoTimestamp The ISO-8601 timestamp string.
   * @return The extracted LocalDate.
   */
  private LocalDate extractLocalDate(String isoTimestamp) {
    return Instant.parse(isoTimestamp).atZone(ZoneOffset.UTC).toLocalDate();
  }
}
