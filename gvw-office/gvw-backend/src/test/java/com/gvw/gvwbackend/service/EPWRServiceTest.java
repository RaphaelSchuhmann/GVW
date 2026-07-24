package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.response.NewEmergencyTokenDTO;
import com.gvw.gvwbackend.exception.InvalidCredentialsException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.EPWRToken;
import com.gvw.gvwbackend.model.User;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class EPWRServiceTest {
  @Mock private DbService dbService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private MailService mailService;

  @Mock private HashUtil hashUtil;

  @InjectMocks private EPWRService epwrService;

  @Test
  void testGetNewEmergencyTokenNoSavedToken() {
    when(hashUtil.createHash(any())).thenReturn("hashedToken");

    when(dbService.findByQuery(any(), any(), eq(EPWRToken.class))).thenReturn(List.of());
    when(dbService.insert(eq("emergency_token"), any(EPWRToken.class))).thenReturn(true);

    NewEmergencyTokenDTO result = epwrService.getNewEmergencyToken();

    assertNotNull(result.token());

    verify(dbService).insert(eq("emergency_token"), any(EPWRToken.class));
  }

  @Test
  void testGetNewEmergencyTokenSavedToken() {
    EPWRToken epwrToken = new EPWRToken();
    epwrToken.setHashedToken("hashedToken");
    epwrToken.setCreatedAt(Instant.now());
    epwrToken.setExpiresAt(Instant.now().plusSeconds(10));

    when(hashUtil.createHash(any())).thenReturn("newHashedToken");

    when(dbService.findByQuery(any(), any(), eq(EPWRToken.class))).thenReturn(List.of(epwrToken));
    when(dbService.insert(eq("emergency_token"), any(EPWRToken.class))).thenReturn(true);

    NewEmergencyTokenDTO result = epwrService.getNewEmergencyToken();

    assertNotNull(result.token());

    verify(dbService).insert(eq("emergency_token"), any(EPWRToken.class));
  }

  @Test
  void testUseEmergencyTokenSucceeds() {
    String rawToken = "plain-token";
    String storedHash = "stored-hash";

    EPWRToken storedToken = new EPWRToken();
    storedToken.setHashedToken(storedHash);
    storedToken.setExpiresAt(Instant.now().plusSeconds(3600));

    User admin1 = new User();
    admin1.setEmail("admin1@mail.com");

    User admin2 = new User();
    admin2.setEmail("admin2@mail.com");

    List<User> admins = List.of(admin1, admin2);

    when(dbService.findByQuery(eq("emergency_token"), any(), eq(EPWRToken.class)))
        .thenReturn(List.of(storedToken));

    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(admins);

    when(hashUtil.compare(rawToken, storedHash)).thenReturn(true);
    when(dbService.insert(eq("emergency_token"), any(EPWRToken.class))).thenReturn(true);

    when(passwordEncoder.encode(any())).thenReturn("encoded-password");

    when(dbService.insert(eq("users"), any(User.class))).thenReturn(true);

    NewEmergencyTokenDTO result = epwrService.useEmergencyToken(rawToken);

    assertNotNull(result);
    assertNotNull(result.token());

    verify(dbService, times(2)).insert(eq("users"), any(User.class));

    verify(passwordEncoder, times(2)).encode(any());

    verify(mailService, times(2))
        .sendMail(
            anyString(), eq("GVW-Office: Passwort zurückgesetzt"), eq("resetPassword"), any());

    verify(mailService, times(2))
        .sendMail(anyString(), eq("Notfallzugang verwendet"), eq("emergencyTokenUsed"), any());

    verify(dbService).insert(eq("emergency_token"), eq(storedToken));
  }

  @Test
  void testUseEmergencyTokenShouldThrowTokenNotFound() {
    when(dbService.findByQuery(any(), any(), eq(EPWRToken.class))).thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> {
          epwrService.useEmergencyToken("token");
        });
  }

  @Test
  void testUseEmergencyTokenShouldThrowTokenExpired() {
    EPWRToken savedToken = new EPWRToken();
    savedToken.setExpiresAt(Instant.now());

    when(dbService.findByQuery(any(), any(), eq(EPWRToken.class))).thenReturn(List.of(savedToken));

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          epwrService.useEmergencyToken("token");
        });
  }

  @Test
  void testUseEmergencyTokenShouldThrowInvalidToken() {
    EPWRToken savedToken = new EPWRToken();
    savedToken.setHashedToken("hashedToken");
    savedToken.setExpiresAt(Instant.now().plusSeconds(3600));

    when(dbService.findByQuery(any(), any(), eq(EPWRToken.class))).thenReturn(List.of(savedToken));

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          epwrService.useEmergencyToken("token");
        });
  }
}
