package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.exception.InvalidCredentialsException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.EPWRToken;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import com.gvw.gvwbackend.util.HashUtil;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class EPWRServiceTest {

  @Mock private DbService dbService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private MailService mailService;

  @Mock private HashUtil hashUtil;

  @InjectMocks private EPWRService epwrService;

  private EPWRToken epwrToken;

  @BeforeEach
  void setUp() {
    epwrToken = new EPWRToken();
    epwrToken.setId("token-1");
    epwrToken.setHashedToken("hashed-token");
    epwrToken.setCreatedAt(Instant.now());
    epwrToken.setExpiresAt(Instant.now().plusSeconds(2592000)); // 30 days
  }

  @Test
  void getNewEmergencyToken_NewToken_Success() {
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of());
    when(hashUtil.createHash(anyString())).thenReturn("new-hashed-token");

    String result = epwrService.getNewEmergencyToken();

    assertNotNull(result);
    assertFalse(result.isEmpty());
    verify(dbService).insert(eq("emergency_token"), any(EPWRToken.class));
  }

  @Test
  void getNewEmergencyToken_ReplaceExisting_Success() {
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of(epwrToken));
    when(hashUtil.createHash(anyString())).thenReturn("new-hashed-token");

    String result = epwrService.getNewEmergencyToken();

    assertNotNull(result);
    verify(dbService).insert(eq("emergency_token"), any(EPWRToken.class));
  }

  @Test
  void getNewEmergencyToken_InsertFailure_ThrowsRuntimeException() {
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of());
    when(hashUtil.createHash(anyString())).thenReturn("new-hashed-token");
    doThrow(new RuntimeException("DB error"))
        .when(dbService)
        .insert(eq("emergency_token"), any(EPWRToken.class));

    assertThrows(RuntimeException.class, () -> epwrService.getNewEmergencyToken());
  }

  @Test
  void useEmergencyToken_Success() {
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of(epwrToken));
    when(hashUtil.compare("valid-token", "hashed-token")).thenReturn(true);
    when(hashUtil.createHash(anyString())).thenReturn("rotated-hash");

    when(dbService.findByQuery("users", Map.of("selector", Map.of("role", Role.ADMIN)), User.class))
        .thenReturn(List.of());

    String result = epwrService.useEmergencyToken("valid-token");

    assertNotNull(result);
    ArgumentCaptor<EPWRToken> captor = ArgumentCaptor.forClass(EPWRToken.class);
    verify(dbService).insert(eq("emergency_token"), captor.capture());
    assertEquals("rotated-hash", captor.getValue().getHashedToken());
  }

  @Test
  void useEmergencyToken_TokenNotFound_ThrowsNotFound() {
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of());

    assertThrows(NotFoundException.class, () -> epwrService.useEmergencyToken("invalid-token"));
  }

  @Test
  void useEmergencyToken_TokenExpired_ThrowsInvalidCredentials() {
    epwrToken.setExpiresAt(Instant.now().minusSeconds(3600));
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of(epwrToken));

    assertThrows(
        InvalidCredentialsException.class, () -> epwrService.useEmergencyToken("expired-token"));
  }

  @Test
  void useEmergencyToken_InvalidHash_ThrowsInvalidCredentials() {
    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of(epwrToken));
    when(hashUtil.compare("invalid-token", "hashed-token")).thenReturn(false);

    assertThrows(
        InvalidCredentialsException.class, () -> epwrService.useEmergencyToken("invalid-token"));
  }

  @Test
  void useEmergencyToken_WithAdmins_ResetsPasswords() {
    User admin = new User();
    admin.setId("admin-1");
    admin.setEmail("admin@example.com");
    admin.setPassword("old-password");
    admin.setChangePassword(false);

    when(dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class))
        .thenReturn(List.of(epwrToken));
    when(hashUtil.compare("valid-token", "hashed-token")).thenReturn(true);
    when(dbService.findByQuery("users", Map.of("selector", Map.of("role", Role.ADMIN)), User.class))
        .thenReturn(List.of(admin));
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

    String result = epwrService.useEmergencyToken("valid-token");

    assertNotNull(result);
    verify(passwordEncoder).encode(anyString());
    verify(mailService, times(2))
        .sendMail(eq("admin@example.com"), anyString(), anyString(), anyMap());
  }
}
