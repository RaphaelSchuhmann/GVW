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
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

  public HttpUtil(HttpClient httpClient) {
    this.httpClient = httpClient;
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
    } catch (Exception e) {
      log.debug("Request failed for URL: {}", url, e);
      return new GVWHttpResponse(500, null, null, true);
    }
  }

  private String extractCharset(String contentType) {
    if (contentType != null) {
      for (String param : contentType.split(";")) {
        param = param.trim();
        if (param.toLowerCase().startsWith("charset=")) {
          return param.substring("charset=".length()).replace("\"", "");
        }
      }
    }
    return StandardCharsets.UTF_8.name();
  }
}
