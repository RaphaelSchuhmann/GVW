package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.exception.ConflictException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class DbServiceTest {

  @Mock private RestTemplate restTemplate;

  private DbService dbService;

  @BeforeEach
  void setup() {
    dbService = new DbService("http://localhost:5984", restTemplate);
  }

  @Test
  void testInsertSuccess() {
    Map<String, Object> response = Map.of("ok", true);
    lenient()
        .when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
        .thenReturn(response);

    assertDoesNotThrow(() -> dbService.insert("test_db", Map.of("name", "test")));
  }

  @Test
  void testInsertFailure() {
    lenient()
        .when(restTemplate.postForObject(anyString(), any(), eq(Map.class)))
        .thenReturn(Map.of("ok", false));

    assertThrows(RuntimeException.class, () -> dbService.insert("test_db", Map.of("name", "John")));
  }

  @Test
  void testUpdateSuccess() {
    Map<String, Object> couchResponse = Map.of("ok", true, "rev", "2-newrev");
    ResponseEntity<Map> responseEntity = new ResponseEntity<>(couchResponse, HttpStatus.OK);

    lenient()
        .when(
            restTemplate.exchange(
                anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class)))
        .thenReturn(responseEntity);

    String result = dbService.update("test_db", "test-id", Map.of("name", "newName"));

    assertNotNull(result);
    assertEquals("2-newrev", result);
  }

  @Test
  void testUpdateConflict() {
    lenient()
        .when(
            restTemplate.exchange(
                anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class)))
        .thenThrow(
            HttpClientErrorException.create(HttpStatus.CONFLICT, "Conflict", null, null, null));

    assertThrows(
        ConflictException.class,
        () -> {
          dbService.update("test_db", "test-id", Map.of("name", "John"));
        });
  }

  @Test
  void testDeleteSuccess() {
    Map<String, Object> response = Map.of("ok", true);
    lenient()
        .when(
            restTemplate.exchange(
                anyString(), eq(org.springframework.http.HttpMethod.DELETE), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(response));

    assertDoesNotThrow(() -> dbService.delete("test_db", "doc_id", "rev"));
  }

  @Test
  void testDeleteFailure() {
    lenient()
        .when(
            restTemplate.exchange(
                anyString(), eq(org.springframework.http.HttpMethod.DELETE), any(), eq(Map.class)))
        .thenReturn(ResponseEntity.ok(Map.of("ok", false)));

    assertThrows(RuntimeException.class, () -> dbService.delete("test_db", "doc_id", "rev"));
  }

  @Test
  void testFindByIdReturnsMap() {
    String json = "{\"_id\":\"1\",\"name\":\"John\"}";
    when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(json);

    Map<String, Object> result = dbService.findById("test_db", "1", Map.class);

    assertEquals("1", result.get("_id"));
    assertEquals("John", result.get("name"));
  }

  @Test
  void testFindByQueryReturnsMap() {
    String json =
        """
            {
              "docs": [
                {"_id": "1", "name": "John"},
                {"_id": "2", "name": "Jane"}
              ],
              "execution_stats": {"total_docs_examined": 2}
            }
            """;

    when(restTemplate.postForObject(anyString(), any(Map.class), eq(String.class)))
        .thenReturn(json);

    List<Map> results = dbService.findByQuery("test_db", Map.of("selector", Map.of()), Map.class);

    assertEquals(2, results.size());
    assertEquals("John", results.get(0).get("name"));
    assertEquals("Jane", results.get(1).get("name"));
  }
}
