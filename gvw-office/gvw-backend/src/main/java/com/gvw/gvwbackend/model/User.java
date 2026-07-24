package com.gvw.gvwbackend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
@EqualsAndHashCode(exclude = "password")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
  @JsonProperty("_id")
  private String id;

  @JsonProperty("_rev")
  private String rev;

  private String email;
  private String name;
  private String password;
  private String phone;
  private String address;

  private Boolean changePassword;
  private Boolean firstLogin;

  private String userId;
  private Role role;
  private String memberId;
  private Integer failedLoginAttempts;

  private Instant lockUntil;

  @Builder.Default private final String type = "user";
}
