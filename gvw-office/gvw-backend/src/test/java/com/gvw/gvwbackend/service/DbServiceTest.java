package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.DatabaseConnectionException;
import com.gvw.gvwbackend.exception.DatabaseMappingException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DbServiceTest {

  @Mock private RestTemplate restTemplate;

  @InjectMocks private DbService dbService;

  private static final String BASE_URL = "http://localhost:5984";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(dbService, "baseUrl", BASE_URL);
  }

  @Test
  void insert_Success() {
    Map<String, Object> doc = Map.of("test", "value");
    Map<String, Object> response = Map.of("ok", true, "id", "doc-1", "rev", "1-abc");
    when(restTemplate.postForObject(eq(BASE_URL + "/test_db"), eq(doc), eq(Map.class)))
        .thenReturn(response);

    assertDoesNotThrow(() -> dbService.insert("test_db", doc));
  }

  @Test
  void insert_ResponseNotOk_ThrowsRuntimeException() {
    Map<String, Object> doc = Map.of("test", "value");
    Map<String, Object> response = Map.of("ok", false);
    when(restTemplate.postForObject(eq(BASE_URL + "/test_db"), eq(doc), eq(Map.class)))
        .thenReturn(response);

    assertThrows(RuntimeException.class, () -> dbService.insert("test_db", doc));
  }

  @Test
  void insert_ConnectionRefused_ThrowsDatabaseConnectionException() {
    Map<String, Object> doc = Map.of("test", "value");
    ResourceAccessException ex =
        new ResourceAccessException("Connection refused", new ConnectException());
    when(restTemplate.postForObject(eq(BASE_URL + "/test_db"), eq(doc), eq(Map.class)))
        .thenThrow(ex);

    assertThrows(DatabaseConnectionException.class, () -> dbService.insert("test_db", doc));
  }

  @Test
  void update_Success() {
    Map<String, Object> doc = Map.of("_id", "doc-1", "_rev", "1-abc", "test", "value");
    ResponseEntity<Map> response = ResponseEntity.ok(Map.of("ok", true, "rev", "2-def"));
    when(restTemplate.exchange(
            eq(BASE_URL + "/test_db/doc-1"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(Map.class)))
        .thenReturn(response);

    String result = dbService.update("test_db", "doc-1", doc);

    assertEquals("2-def", result);
  }

  @Test
  void update_Conflict_ThrowsConflictException() {
    Map<String, Object> doc = Map.of("_id", "doc-1", "_rev", "1-abc", "test", "value");
    when(restTemplate.exchange(
            eq(BASE_URL + "/test_db/doc-1"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(Map.class)))
        .thenThrow(
            HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", HttpHeaders.EMPTY, null, null));

    assertThrows(ConflictException.class, () -> dbService.update("test_db", "doc-1", doc));
  }

  @Test
  void update_EmptyBody_ThrowsDatabaseConnectionException() {
    Map<String, Object> doc = Map.of("_id", "doc-1", "_rev", "1-abc", "test", "value");
    ResponseEntity<Map> response = ResponseEntity.ok(null);
    when(restTemplate.exchange(
            eq(BASE_URL + "/test_db/doc-1"),
            eq(HttpMethod.PUT),
            any(HttpEntity.class),
            eq(Map.class)))
        .thenReturn(response);

    assertThrows(
        DatabaseConnectionException.class, () -> dbService.update("test_db", "doc-1", doc));
  }

  @Test
  void delete_Success() {
    Map<String, Object> response = Map.of("ok", true);
    when(restTemplate.exchange(
            eq(BASE_URL + "/test_db/doc-1?rev=1-abc"),
            eq(HttpMethod.DELETE),
            isNull(),
            eq(Map.class)))
        .thenReturn(ResponseEntity.ok(response));

    assertDoesNotThrow(() -> dbService.delete("test_db", "doc-1", "1-abc"));
  }

  @Test
  void delete_ResponseNotOk_ThrowsRuntimeException() {
    Map<String, Object> response = Map.of("ok", false);
    when(restTemplate.exchange(
            eq(BASE_URL + "/test_db/doc-1?rev=1-abc"),
            eq(HttpMethod.DELETE),
            isNull(),
            eq(Map.class)))
        .thenReturn(ResponseEntity.ok(response));

    assertThrows(RuntimeException.class, () -> dbService.delete("test_db", "doc-1", "1-abc"));
  }

  @Test
  void findAll_Success() {
    Map<String, Object> response =
        Map.of(
            "rows",
            List.of(
                Map.of("doc", Map.of("_id", "doc-1", "test", "value1")),
                Map.of("doc", Map.of("_id", "doc-2", "test", "value2"))));
    when(restTemplate.getForObject(
            eq(BASE_URL + "/test_db/_all_docs?include_docs=true"), eq(Map.class)))
        .thenReturn(response);

    List<Map> result = dbService.findAll("test_db", Map.class);

    assertEquals(2, result.size());
  }

  @Test
  void findAll_EmptyRows_ReturnsEmptyList() {
    Map<String, Object> response = Map.of("rows", List.of());
    when(restTemplate.getForObject(
            eq(BASE_URL + "/test_db/_all_docs?include_docs=true"), eq(Map.class)))
        .thenReturn(response);

    List<Map> result = dbService.findAll("test_db", Map.class);

    assertTrue(result.isEmpty());
  }

  @Test
  void findById_Success() {
    String json = "{\"_id\":\"doc-1\",\"test\":\"value\"}";
    when(restTemplate.getForObject(eq(BASE_URL + "/test_db/doc-1"), eq(String.class)))
        .thenReturn(json);

    Map<String, Object> result = dbService.findById("test_db", "doc-1", Map.class);

    assertEquals("doc-1", result.get("_id"));
    assertEquals("value", result.get("test"));
  }

  @Test
  void findById_NotFound_ReturnsNull() {
    when(restTemplate.getForObject(eq(BASE_URL + "/test_db/doc-1"), eq(String.class)))
        .thenReturn(null);

    Map<String, Object> result = dbService.findById("test_db", "doc-1", Map.class);

    assertNull(result);
  }

  @Test
  void findById_InvalidJson_ThrowsDatabaseMappingException() {
    String json = "invalid json";
    when(restTemplate.getForObject(eq(BASE_URL + "/test_db/doc-1"), eq(String.class)))
        .thenReturn(json);

    assertThrows(
        DatabaseMappingException.class, () -> dbService.findById("test_db", "doc-1", Map.class));
  }

  @Test
  void findByQuery_Success() {
    Map<String, Object> query = Map.of("selector", Map.of("test", "value"));
    String json = "{\"docs\":[{\"_id\":\"doc-1\",\"test\":\"value1\"}]}";
    when(restTemplate.postForObject(eq(BASE_URL + "/test_db/_find"), eq(query), eq(String.class)))
        .thenReturn(json);

    List<Map> result = dbService.findByQuery("test_db", query, Map.class);

    assertEquals(1, result.size());
    assertEquals("doc-1", result.get(0).get("_id"));
  }

  @Test
  void findByQuery_EmptyResponse_ReturnsEmptyList() {
    Map<String, Object> query = Map.of("selector", Map.of("test", "value"));
    when(restTemplate.postForObject(eq(BASE_URL + "/test_db/_find"), eq(query), eq(String.class)))
        .thenReturn(null);

    List<Map> result = dbService.findByQuery("test_db", query, Map.class);

    assertTrue(result.isEmpty());
  }

  @Test
  void findByQuery_InvalidJson_ThrowsDatabaseMappingException() {
    Map<String, Object> query = Map.of("selector", Map.of("test", "value"));
    when(restTemplate.postForObject(eq(BASE_URL + "/test_db/_find"), eq(query), eq(String.class)))
        .thenReturn("invalid json");

    assertThrows(
        DatabaseMappingException.class, () -> dbService.findByQuery("test_db", query, Map.class));
  }

  @Test
  void isConnectionRefused_ConnectException_ReturnsTrue() {
    ConnectException ex = new ConnectException("Connection refused");
    DbService dbServiceSpy = spy(dbService);

    boolean result = dbServiceSpy.isConnectionRefused(ex);

    assertTrue(result);
  }

  @Test
  void isConnectionRefused_NoConnectException_ReturnsFalse() {
    RuntimeException ex = new RuntimeException("Other error");
    DbService dbServiceSpy = spy(dbService);

    boolean result = dbServiceSpy.isConnectionRefused(ex);

    assertFalse(result);
  }
}
