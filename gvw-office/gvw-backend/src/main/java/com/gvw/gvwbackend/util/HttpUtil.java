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

/**
 * Utility class for making HTTP requests with automatic JSON serialization and deserialization.
 *
 * <p>This component provides a simplified interface for HTTP GET and POST operations, handling:
 *
 * <ul>
 *   <li>JSON request body serialization for POST requests
 *   <li>JSON response body deserialization
 *   <li>Error handling and logging
 *   <li>Standard HTTP headers (User-Agent, Accept, Content-Type)
 * </ul>
 *
 * <p>All methods return a {@link GVWHttpResponse} wrapper containing the status code, parsed body,
 * content type, and error state.
 */
@Component
public class HttpUtil {
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

  /**
   * Constructs a new HttpUtil with the specified HTTP client and object mapper.
   *
   * @param httpClient the HTTP client to use for sending requests
   * @param objectMapper the object mapper for JSON serialization/deserialization
   */
  public HttpUtil(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  /**
   * Sends an HTTP GET request to the specified URL.
   *
   * <p>The response body is automatically deserialized to the specified type if the request is
   * successful (status code < 400) and the response type is not null.
   *
   * @param url the target URL to send the GET request to
   * @param responseType the class type to deserialize the response body to, or null to keep as raw
   *     string
   * @param <T> the type of the deserialized response body
   * @return a GVWHttpResponse containing the status code, parsed body, content type, and error state
   */
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

  /**
   * Sends an HTTP POST request with a JSON body to the specified URL.
   *
   * <p>The request body is automatically serialized to JSON. The response body is automatically
   * deserialized to the specified type if the request is successful (status code < 400) and the
   * response type is not null.
   *
   * @param url the target URL to send the POST request to
   * @param requestBody the object to serialize as the JSON request body
   * @param responseType the class type to deserialize the response body to, or null to keep as raw
   *     string
   * @param <T> the type of the request body
   * @param <U> the type of the deserialized response body
   * @return a GVWHttpResponse containing the status code, parsed body, content type, and error state
   */
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

  /**
   * Extracts and processes an HTTP response into a GVWHttpResponse wrapper.
   *
   * <p>This method parses the response body to the specified type if the response is successful,
   * otherwise it returns the raw body as a string. It also determines the error state based on the
   * status code (>= 400 is considered an error).
   *
   * @param response the raw HTTP response to process
   * @param responseType the class type to deserialize the response body to, or null to keep as raw
   *     string
   * @param <T> the type of the deserialized response body
   * @return a GVWHttpResponse containing the status code, parsed body, content type, and error state
   */
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
