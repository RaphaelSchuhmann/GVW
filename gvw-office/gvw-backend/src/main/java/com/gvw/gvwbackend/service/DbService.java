package com.gvw.gvwbackend.service;

import static net.logstash.logback.argument.StructuredArguments.kv;

import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.DatabaseConnectionException;
import com.gvw.gvwbackend.exception.DatabaseMappingException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class DbService {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(DbService.class);

  private final RestTemplate restTemplate;
  private final String baseUrl;

  private final String[] databases = {
    "users",
    "members",
    "events",
    "reports",
    "app_settings",
    "emergency_token",
    "library",
    "changelogs",
    "help_center"
  };
  private final String appSettingsDb = "app_settings";
  private Map<String, Object> DEFAULT_SETTINGS = new HashMap<>();

  public DbService(
      @Value("${couchdb.url}") String baseUrl,
      @Qualifier("dbRestTemplate") RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
    this.baseUrl = baseUrl;

    // Default settings
    DEFAULT_SETTINGS.put("_id", "general");
    DEFAULT_SETTINGS.put("maxMembers", 10);

    Map<String, String> scoreCategories = new HashMap<>();
    scoreCategories.put("", "all");
    scoreCategories.put("all", "Alle Kategorien");
    scoreCategories.put("Alle Kategorien", "all");

    DEFAULT_SETTINGS.put("scoreCategories", scoreCategories);
    log.info("DB Service initialized");
  }

  public <T> boolean insert(String db, T doc) {
    String url = String.format("%s/%s", baseUrl, db);
    Map<String, Object> resp =
        safeExecute(() -> restTemplate.postForObject(url, doc, Map.class), db);
    return resp != null && Boolean.TRUE.equals(resp.get("ok"));
  }

  public <T> Map<String, Object> update(String db, String id, T doc) {
    String url = String.format("%s/%s/%s", baseUrl, db, id);

    HttpEntity<T> requestEntity = new HttpEntity<>(doc);

    return safeExecute(
        () -> {
          try {
            ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Map.class);

            Map body = response.getBody();
            if (body == null) {
              throw new DatabaseConnectionException("UpdateReturnedEmptyResponse");
            }
            return body;
          } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException("RevisionMismatch");
          }
        },
        db);
  }

  public boolean delete(String db, String id, String rev) {
    String url = String.format("%s/%s/%s?rev=%s", baseUrl, db, id, rev);
    Map<String, Object> resp =
        safeExecute(
            () -> restTemplate.exchange(url, HttpMethod.DELETE, null, Map.class).getBody(), db);
    return resp != null && Boolean.TRUE.equals(resp.get("ok"));
  }

  public List<Map<String, Object>> findAll(String db) {
    String url = String.format("%s/%s/_all_docs?include_docs=true", baseUrl, db);
    Map<String, Object> resp = safeExecute(() -> restTemplate.getForObject(url, Map.class), db);

    if (resp == null || !resp.containsKey("rows")) {
      return List.of();
    }

    List<Map<String, Object>> docs = new ArrayList<>();
    List<Map<String, Object>> rows = (List<Map<String, Object>>) resp.get("rows");

    for (Map<String, Object> row : rows) {
      Map<String, Object> doc = (Map<String, Object>) row.get("doc");
      if (doc != null) docs.add(doc);
    }

    return docs;
  }

  public <T> T findById(String db, String id, Class<T> clazz) {
    String url = String.format("%s/%s/%s", baseUrl, db, id);
    String json = safeExecute(() -> restTemplate.getForObject(url, String.class), db);
    if (json == null || json.isEmpty()) return null;

    try {
      return objectMapper.readValue(json, clazz);
    } catch (Exception e) {
      log.error("Failed to map JSON to {}", clazz.getSimpleName(), e);
      throw new DatabaseMappingException("Failed to map JSON to " + clazz.getSimpleName(), e);
    }
  }

  public <T> List<T> findByQuery(String db, Map<String, Object> query, Class<T> clazz) {
    String url = String.format("%s/%s/_find", baseUrl, db);
    String resp = safeExecute(() -> restTemplate.postForObject(url, query, String.class), db);

    if (resp == null || resp.isEmpty()) return List.of();

    try {
      ObjectMapper objectMapper = new ObjectMapper();

      Map<String, Object> map = objectMapper.readValue(resp, new TypeReference<>() {});
      List<Map<String, Object>> docs = (List<Map<String, Object>>) map.get("docs");

      if (docs == null) return List.of();

      return docs.stream()
          .map(doc -> objectMapper.convertValue(doc, clazz))
          .collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Failed to map JSON to {}", clazz.getSimpleName(), e);
      throw new DatabaseMappingException("Failed to map JSON to " + clazz.getSimpleName(), e);
    }
  }

  public void createDatabases() {
    for (String database : databases) {
      String url = String.format("%s/%s", baseUrl, database);

      try {
        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, null, Void.class);
      } catch (ResourceAccessException e) {
        if (isConnectionRefused(e)) {
          log.error("DB connection refused {} {}", kv("db", database), kv("error", "ECONNREFUSED"));
        } else {
          log.error(
              "DB network error {} {}",
              kv("db", database),
              kv("error", e.getCause().getMessage()),
              e);
        }
        throw new DatabaseConnectionException("DB connection failed", e);
      } catch (HttpStatusCodeException e) {
        if (e.getStatusCode() == HttpStatusCode.valueOf(412)) {
          log.debug("DB already exists {}", kv("db", database));
          continue;
        }

        log.error(
            "DB creation failed {} {}",
            kv("db", database),
            kv("status", e.getStatusCode().value()),
            e);
        throw new DatabaseConnectionException("DB creation failed", e);
      } catch (Exception e) {
        log.error("Unexpected db error {} {}", kv("db", database), kv("error", e.getMessage()), e);
        throw new DatabaseConnectionException("Unexpected DB error", e);
      }
    }

    // Ensure default settings document exists in app_settings
    // Delete existing default settings document
    String url = String.format("%s/%s/general", baseUrl, appSettingsDb);
    ResponseEntity<Map> resp = null;
    try {
      resp = restTemplate.getForEntity(url, Map.class);
    } catch (HttpStatusCodeException e) {
      if (e.getStatusCode().value() != 404) {
        throw new DatabaseConnectionException("DB request failed", e);
      }
    }

    if (resp != null && resp.getStatusCode().is2xxSuccessful()) {
      Map<String, Object> existingDoc = resp.getBody();
      if (existingDoc != null) {
        delete(appSettingsDb, (String) existingDoc.get("_id"), (String) existingDoc.get("_rev"));
      }
    }

    boolean successful = insert(appSettingsDb, DEFAULT_SETTINGS);
    if (!successful) {
      log.error("Failed to insert default settings document {}", kv("db", appSettingsDb));
      return;
    }

    log.info("Inserted default settings document {}", kv("db", appSettingsDb));
  }

  private <T> T safeExecute(Supplier<T> action, String db) {
    try {
      return action.get();
    } catch (ConflictException e) {
      throw e;
    } catch (ResourceAccessException e) {
      if (isConnectionRefused(e)) {
        log.error("DB connection refused {} {}", kv("db", db), kv("error", "ECONNREFUSED"));
      } else {
        log.error(
            "DB network error {} {}", kv("db", db), kv("error", e.getCause().getMessage()), e);
      }
      throw new DatabaseConnectionException("DB connection failed", e);
    } catch (HttpStatusCodeException e) {
      // Allow 404 returns to be actually thrown
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        log.debug("Document not found in DB {} {}", kv("db", db), kv("status", 404));
        throw e;
      }

      log.error(
          "DB request failed {} {}", kv("db", db), kv("status", e.getStatusCode().value()), e);
      throw new DatabaseConnectionException("DB request failed", e);
    } catch (Exception e) {
      log.error("Unexpected db error {} {}", kv("db", db), kv("error", e.getMessage()), e);
      throw new DatabaseConnectionException("Unexpected DB error", e);
    }
  }

  private boolean isConnectionRefused(Throwable e) {
    while (e != null) {
      if (e instanceof ConnectException) return true;
      e = e.getCause();
    }
    return false;
  }
}
