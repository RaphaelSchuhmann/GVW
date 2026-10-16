package com.gvw.gvwbackend.util;

import com.gvw.gvwbackend.model.GVWHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class HttpUtil {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

  public HttpUtil(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  public <T> GVWHttpResponse sendGetRequest(String url, Class<T> responseType) {
    if (url == null || url.isBlank()) {
      return new GVWHttpResponse(400, null, null, true);
    }

    HttpRequest request =
            HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Java-GVW-Http")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

    try {
      HttpResponse<String> response =
              httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return extractResponse(response, responseType);
    } catch (Exception e) {
      log.debug("GET request failed for URL: {}", url, e);
      return new GVWHttpResponse(500, null, null, true);
    }
  }

  public <T, U> GVWHttpResponse sendPostRequest(String url, T requestBody, Class<U> responseType) {
    if (url == null || url.isBlank()) {
      return new GVWHttpResponse(400, null, null, true);
    }

    try {
      String jsonPayload = objectMapper.writeValueAsString(requestBody);

      HttpRequest request =
              HttpRequest.newBuilder()
                      .uri(URI.create(url))
                      .header("User-Agent", "Java-GVW-Http")
                      .header("Content-Type", "application/json")
                      .header("Accept", "application/json")
                      .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                      .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return extractResponse(response, responseType);
    } catch (Exception e) {
      log.debug("POST request failed for URL: " + url, e);
      return new GVWHttpResponse(500, null, null, true);
    }
  }

  private <T> GVWHttpResponse extractResponse (HttpResponse<String> response, Class<T> responseType) {
    if (response == null) {
      return new GVWHttpResponse(500, null, null, true);
    }

    int status = response.statusCode();
    boolean isError = status >= 400;
    String rawBody = response.body();

    Object parsedBody;
    if (!isError && rawBody != null && !rawBody.isBlank() && responseType != null) {
      parsedBody = objectMapper.readValue(rawBody, responseType);
    } else {
      parsedBody = rawBody;
    }

    String contentType = response.headers().firstValue("Content-Type").orElse(null);

    return new GVWHttpResponse(status, parsedBody, contentType, isError);
  }
}
