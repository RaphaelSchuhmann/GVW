package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EPWRToken {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String hashedToken;
  private Instant createdAt;
  private Instant expiresAt;

  @Builder.Default private final String type = "epwrToken";
}
