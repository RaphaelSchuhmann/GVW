package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.response.DashboardEventSummaryDTO;
import com.gvw.gvwbackend.dto.response.DashboardMemberSummaryDTO;
import com.gvw.gvwbackend.dto.response.DashboardResponseDTO;
import com.gvw.gvwbackend.model.Event;
import com.gvw.gvwbackend.model.Member;
import com.gvw.gvwbackend.model.Score;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class DashboardService {
  private final DbService dbService;
  private final ObjectMapper mapper = new ObjectMapper();

  public DashboardService(DbService dbService) {
    this.dbService = dbService;
  }

  public DashboardResponseDTO getData() {
    // Get members
    List<Map<String, Object>> membersRaw = dbService.findAll("members");

    List<Member> members =
        membersRaw.stream().map(map -> mapper.convertValue(map, Member.class)).toList();

    List<DashboardMemberSummaryDTO> responseMemberData =
        members.stream()
            .map(m -> new DashboardMemberSummaryDTO(m.getStatus(), m.getVoice()))
            .toList();

    // Get events
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

    // Get scores
    List<Map<String, Object>> rawScores = dbService.findAll("library");

    List<Score> scores =
        rawScores.stream().map(map -> mapper.convertValue(map, Score.class)).toList();

    return new DashboardResponseDTO(
        responseMemberData, events.size(), responseUpcomingEventData, scores.size());
  }
}
