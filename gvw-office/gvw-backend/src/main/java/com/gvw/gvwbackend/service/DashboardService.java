package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.response.AdminDashboardResponseDTO;
import com.gvw.gvwbackend.dto.response.DashboardEventSummaryDTO;
import com.gvw.gvwbackend.dto.response.DashboardMemberSummaryDTO;
import com.gvw.gvwbackend.dto.response.DashboardResponseDTO;
import com.gvw.gvwbackend.model.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Service responsible for preparing dashboard data.
 *
 * <p>The dashboard does not store its own data. Instead, it aggregates information from multiple
 * CouchDB collections and creates lightweight summary DTOs used by the frontend dashboard.
 *
 * <p>Currently aggregated data:
 *
 * <ul>
 *   <li>Member status and voice distribution
 *   <li>Upcoming events
 *   <li>Total number of scores in the library
 * </ul>
 */
@Service
public class DashboardService {
  private final DbService dbService;
  private final ObjectMapper mapper = new ObjectMapper();

  public DashboardService(DbService dbService) {
    this.dbService = dbService;
  }

  /**
   * Loads and aggregates all data required for the dashboard view.
   *
   * <p>This method intentionally returns summary information instead of full documents to avoid
   * sending unnecessary data to the frontend.
   *
   * <p>Upcoming events are filtered by status and sorted by date so that the dashboard always
   * displays the nearest events first.
   *
   * @return aggregated dashboard data
   */
  public DashboardResponseDTO getUserDashboardData() {
    // Load only member fields required for dashboard statistics.
    // Full member data is intentionally not exposed here.
    List<Map<String, Object>> membersRaw = dbService.findAll("members");

    List<Member> members =
        membersRaw.stream().map(map -> mapper.convertValue(map, Member.class)).toList();

    List<DashboardMemberSummaryDTO> responseMemberData =
        members.stream()
            .map(m -> new DashboardMemberSummaryDTO(m.getStatus(), m.getVoice()))
            .toList();

    // Only upcoming events are relevant for the dashboard preview.
    // Finished events remain available through the event management view.
    List<Map<String, Object>> eventsRaw = dbService.findAll("events");

    List<Event> events =
        eventsRaw.stream().map(map -> mapper.convertValue(map, Event.class)).toList();

    List<Event> upcomingEvents =
        events.stream()
            .filter(event -> "upcoming".equals(event.getStatus()))
            .sorted(
                Comparator.comparing(
                    Event::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    List<DashboardEventSummaryDTO> responseUpcomingEventData =
        upcomingEvents.stream()
            .map(
                m ->
                    new DashboardEventSummaryDTO(
                        m.getTitle(),
                        m.getDate(),
                        m.getTime(),
                        m.getLocation(),
                        m.getType(),
                        m.getMode(),
                        m.getRecurrence()))
            .toList();

    List<Map<String, Object>> rawScores = dbService.findAll("library");

    List<Score> scores =
        rawScores.stream().map(map -> mapper.convertValue(map, Score.class)).toList();

    return new DashboardResponseDTO(
        responseMemberData, events.size(), responseUpcomingEventData, scores.size());
  }

  /**
   * Loads and aggregates all data required for the admin dashboard view with the exception of
   * changelogs.
   *
   * <p>This method intentionally returns summary information instead of full documents to avoid
   * sending unnecessary data to the frontend.
   *
   * @return aggregated admin dashboard data
   */
  public AdminDashboardResponseDTO getAdminDashboardData() {
    List<Map<String, Object>> feedbacksRaw = dbService.findAll("feedbacks");
    List<UserFeedback> feedbacks =
        feedbacksRaw.stream().map(map -> mapper.convertValue(map, UserFeedback.class)).toList();

    double averageSentiment =
        feedbacks.stream().mapToInt(UserFeedback::getSentiment).average().orElse(0.0);

    List<Map<String, Object>> bugReportsRaw = dbService.findAll("bug_reports");
    List<BugReport> bugReports =
        bugReportsRaw.stream().map(map -> mapper.convertValue(map, BugReport.class)).toList();

    Optional<String> mostUsedHashOptional =
        bugReports.stream()
            .map(BugReport::getMetaData)
            .filter(Objects::nonNull)
            .map(UserReportMetaData::getRoute)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey);

    String mostUsedHash = "";
    if (mostUsedHashOptional.isPresent()) {
      mostUsedHash = mostUsedHashOptional.get();
    }

    List<Map<String, Object>> usersRaw = dbService.findAll("users");
    List<User> users = usersRaw.stream().map(map -> mapper.convertValue(map, User.class)).toList();

    // Collect non-empty memberIds
    Set<String> memberIds =
        users.stream()
            .map(User::getMemberId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());

    // One bulk lookup against members
    Set<String> existingMemberIds =
        memberIds.isEmpty()
            ? Set.of()
            : dbService
                .findByQuery(
                    "members",
                    Map.of("selector", Map.of("_id", Map.of("$in", memberIds))),
                    Member.class)
                .stream()
                .map(Member::getId)
                .collect(Collectors.toSet());

    long totalOrphaned =
        users.stream()
            .map(User::getMemberId)
            .filter(m -> m == null || m.isBlank() || !existingMemberIds.contains(m))
            .count();

    return new AdminDashboardResponseDTO(
        feedbacks.size(),
        bugReports.size(),
        averageSentiment,
        mostUsedHash,
        users.size(),
        totalOrphaned);
  }
}
