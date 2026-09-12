package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Member {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String name;
  private String surname;
  private String email;
  private String phone;
  private String address;

  private String voice;
  private String status;
  private Role role;

  private String birthdate;
  private String joined;

  private Boolean isMarried = false; // should be false by default
  private String marriedSince;

  @Builder.Default private final String type = "member";
}
