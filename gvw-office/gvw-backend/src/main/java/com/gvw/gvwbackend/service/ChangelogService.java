package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddChangelogRequestDTO;
import com.gvw.gvwbackend.dto.response.ChangelogResponseDTO;
import com.gvw.gvwbackend.dto.response.ChangelogsResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ErrorAction;
import com.gvw.gvwbackend.exception.ErrorDomain;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.Changelog;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ChangelogService {
  private final DbService dbService;
  private final ObjectMapper mapper = new ObjectMapper();
  private final SseService sseService;
  private static final Logger log = LoggerFactory.getLogger(ChangelogService.class);

  public ChangelogService(DbService dbService, SseService sseService) {
    this.dbService = dbService;
    this.sseService = sseService;
  }

  public ChangelogsResponseDTO getChangelogs() {
    List<Map<String, Object>> changelogsRaw = dbService.findAll("changelogs");

    List<Changelog> changelogs =
        changelogsRaw.stream()
            .map(map -> mapper.convertValue(map, Changelog.class))
            .sorted(Comparator.comparing(Changelog::getTimestamp).reversed())
            .toList();

    if (changelogs.isEmpty()) {
      return new ChangelogsResponseDTO(List.of());
    }

    List<ChangelogResponseDTO> responseChangelogs =
        changelogs.stream()
            .map(
                m ->
                    new ChangelogResponseDTO(
                        m.getId(), m.getTitle(), m.getVersion(), m.getContent(), m.getTimestamp()))
            .toList();

    return new ChangelogsResponseDTO(responseChangelogs);
  }

  public void addChangelog(AddChangelogRequestDTO request) {
    Changelog changelog = new Changelog();
    changelog.setVersion(request.version());
    changelog.setTitle(request.title());
    changelog.setContent(request.content());
    changelog.setTimestamp(request.timestamp());

    dbService.insert("changelogs", changelog);

    try {
      sseService.broadcastRefresh("CHANGELOGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast CHANGELOGS refresh: ", ex);
    }
  }

  public void deleteChangelog(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException("InvalidData");
    }

    Changelog changelog = dbService.findById("changelogs", id, Changelog.class);
    if (changelog == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.CHANGELOG.createCode(ErrorAction.DELETE, 404)));
    }

    dbService.delete("changelogs", changelog.getId(), changelog.getRev());

    try {
      sseService.broadcastRefresh("CHANGELOGS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast CHANGELOGS refresh: ", ex);
    }
  }
}
