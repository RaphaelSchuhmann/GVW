package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.model.GVWHttpResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class HttpUtilTest {

  @Mock private HttpClient httpClient;

  @Mock private ObjectMapper objectMapper;

  private HttpUtil httpUtil;

  @BeforeEach
  void setUp() {
    httpUtil = new HttpUtil(httpClient, objectMapper);
  }

  @Test
  void sendGetRequest_NullUrl_ReturnsBadRequest() {
    GVWHttpResponse response = httpUtil.sendGetRequest(null, String.class);

    assertEquals(400, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendGetRequest_BlankUrl_ReturnsBadRequest() {
    GVWHttpResponse response = httpUtil.sendGetRequest("   ", String.class);

    assertEquals(400, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendGetRequest_Success_ReturnsParsedResponse() throws Exception {
    String url = "http://example.com/api";
    String responseBody = "{\"key\":\"value\"}";
    TestResponse parsedResponse = new TestResponse("value");

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(responseBody);
    when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of("Content-Type", java.util.List.of("application/json")), (k, v) -> true));

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    when(objectMapper.readValue(eq(responseBody), eq(TestResponse.class))).thenReturn(parsedResponse);

    GVWHttpResponse response = httpUtil.sendGetRequest(url, TestResponse.class);

    assertEquals(200, response.status());
    assertFalse(response.isError());
    assertEquals(parsedResponse, response.body());
    assertEquals("application/json", response.contentType());
  }

  @Test
  void sendGetRequest_ErrorStatus_ReturnsError() throws Exception {
    String url = "http://example.com/api";
    String responseBody = "{\"error\":\"Not found\"}";

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(404);
    when(mockResponse.body()).thenReturn(responseBody);
    when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true));

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    GVWHttpResponse response = httpUtil.sendGetRequest(url, String.class);

    assertEquals(404, response.status());
    assertTrue(response.isError());
    assertEquals(responseBody, response.body(), "Error responses should return raw body");
  }

  @Test
  void sendGetRequest_Exception_ReturnsServerError() throws Exception {
    String url = "http://example.com/api";

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new RuntimeException("Connection failed"));

    GVWHttpResponse response = httpUtil.sendGetRequest(url, String.class);

    assertEquals(500, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendGetRequest_NullResponseType_ReturnsRawBody() throws Exception {
    String url = "http://example.com/api";
    String responseBody = "raw string response";

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(responseBody);
    when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true));

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    GVWHttpResponse response = httpUtil.sendGetRequest(url, null);

    assertEquals(200, response.status());
    assertFalse(response.isError());
    assertEquals(responseBody, response.body());
  }

  @Test
  void sendPostRequest_NullUrl_ReturnsBadRequest() {
    GVWHttpResponse response = httpUtil.sendPostRequest(null, new TestRequest("value"), String.class);

    assertEquals(400, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendPostRequest_BlankUrl_ReturnsBadRequest() {
    GVWHttpResponse response = httpUtil.sendPostRequest("   ", new TestRequest("value"), String.class);

    assertEquals(400, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendPostRequest_Success_ReturnsParsedResponse() throws Exception {
    String url = "http://example.com/api";
    TestRequest requestBody = new TestRequest("value");
    String jsonPayload = "{\"field\":\"value\"}";
    String responseBody = "{\"result\":\"success\"}";
    TestResponse parsedResponse = new TestResponse("success");

    when(objectMapper.writeValueAsString(requestBody)).thenReturn(jsonPayload);

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(201);
    when(mockResponse.body()).thenReturn(responseBody);
    when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of("Content-Type", java.util.List.of("application/json")), (k, v) -> true));

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);
    when(objectMapper.readValue(eq(responseBody), eq(TestResponse.class))).thenReturn(parsedResponse);

    GVWHttpResponse response = httpUtil.sendPostRequest(url, requestBody, TestResponse.class);

    assertEquals(201, response.status());
    assertFalse(response.isError());
    assertEquals(parsedResponse, response.body());
    assertEquals("application/json", response.contentType());
  }

  @Test
  void sendPostRequest_ErrorStatus_ReturnsError() throws Exception {
    String url = "http://example.com/api";
    TestRequest requestBody = new TestRequest("value");
    String jsonPayload = "{\"field\":\"value\"}";
    String responseBody = "{\"error\":\"Bad request\"}";

    when(objectMapper.writeValueAsString(requestBody)).thenReturn(jsonPayload);

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(400);
    when(mockResponse.body()).thenReturn(responseBody);
    when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true));

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    GVWHttpResponse response = httpUtil.sendPostRequest(url, requestBody, String.class);

    assertEquals(400, response.status());
    assertTrue(response.isError());
    assertEquals(responseBody, response.body(), "Error responses should return raw body");
  }

  @Test
  void sendPostRequest_SerializationException_ReturnsServerError() throws Exception {
    String url = "http://example.com/api";
    TestRequest requestBody = new TestRequest("value");

    when(objectMapper.writeValueAsString(requestBody))
        .thenThrow(new RuntimeException("Serialization failed"));

    GVWHttpResponse response = httpUtil.sendPostRequest(url, requestBody, String.class);

    assertEquals(500, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendPostRequest_HttpException_ReturnsServerError() throws Exception {
    String url = "http://example.com/api";
    TestRequest requestBody = new TestRequest("value");
    String jsonPayload = "{\"field\":\"value\"}";

    when(objectMapper.writeValueAsString(requestBody)).thenReturn(jsonPayload);
    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenThrow(new RuntimeException("Connection failed"));

    GVWHttpResponse response = httpUtil.sendPostRequest(url, requestBody, String.class);

    assertEquals(500, response.status());
    assertTrue(response.isError());
  }

  @Test
  void sendPostRequest_NullResponseType_ReturnsRawBody() throws Exception {
    String url = "http://example.com/api";
    TestRequest requestBody = new TestRequest("value");
    String jsonPayload = "{\"field\":\"value\"}";
    String responseBody = "raw string response";

    when(objectMapper.writeValueAsString(requestBody)).thenReturn(jsonPayload);

    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn(responseBody);
    when(mockResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(java.util.Map.of(), (k, v) -> true));

    when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
        .thenReturn(mockResponse);

    GVWHttpResponse response = httpUtil.sendPostRequest(url, requestBody, null);

    assertEquals(200, response.status());
    assertFalse(response.isError());
    assertEquals(responseBody, response.body());
  }

  // Helper test classes
  record TestRequest(String field) {}

  record TestResponse(String key) {}
}
