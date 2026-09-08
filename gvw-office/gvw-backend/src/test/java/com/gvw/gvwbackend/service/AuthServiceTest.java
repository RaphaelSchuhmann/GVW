package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.ChangePwRequestDTO;
import com.gvw.gvwbackend.dto.request.LoginRequestDTO;
import com.gvw.gvwbackend.dto.response.AutoLoginResponseDTO;
import com.gvw.gvwbackend.dto.response.LoginResponseDTO;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.InvalidCredentialsException;
import com.gvw.gvwbackend.exception.TooManyRequestsException;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private DbService dbService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  @InjectMocks private AuthService authService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId("user-1");
    user.setUserId("auth-user-1");
    user.setEmail("test@example.com");
    user.setPassword("encodedPassword");
    user.setRole(Role.MEMBER);
    user.setUserActive(true);
    user.setChangePassword(false);
    user.setFirstLogin(false);
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);
  }

  @Test
  void login_Success() {
    LoginRequestDTO request = new LoginRequestDTO("test@example.com", "password");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
    when(jwtService.generateToken("auth-user-1", Map.of("role", "member"))).thenReturn("jwt-token");
    when(dbService.update("users", "user-1", user)).thenReturn("2-rev");

    LoginResponseDTO result = authService.login(request);

    assertEquals("jwt-token", result.authToken());
    assertFalse(result.changePassword());
    assertFalse(result.firstLogin());
    assertEquals("2-rev", result.rev());
    assertEquals(0, user.getFailedLoginAttempts());
    assertNull(user.getLockUntil());
  }

  @Test
  void login_UserNotFound_ThrowsInvalidCredentials() {
    LoginRequestDTO request = new LoginRequestDTO("test@example.com", "password");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of());

    assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
  }

  @Test
  void login_AccountLocked_ThrowsTooManyRequests() {
    user.setLockUntil(Instant.now().plusSeconds(300));
    LoginRequestDTO request = new LoginRequestDTO("test@example.com", "password");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of(user));

    assertThrows(TooManyRequestsException.class, () -> authService.login(request));
  }

  @Test
  void login_WrongPassword_IncrementsFailedAttempts() {
    LoginRequestDTO request = new LoginRequestDTO("test@example.com", "wrong");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);
    when(dbService.update("users", "user-1", user)).thenReturn("2-rev");

    assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    assertEquals(1, user.getFailedLoginAttempts());
  }

  @Test
  void login_FifthFailedAttempt_LocksAccount() {
    user.setFailedLoginAttempts(4);
    LoginRequestDTO request = new LoginRequestDTO("test@example.com", "wrong");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.matches("wrong", "encodedPassword")).thenReturn(false);
    when(dbService.update("users", "user-1", user)).thenReturn("2-rev");

    assertThrows(TooManyRequestsException.class, () -> authService.login(request));
    assertNotNull(user.getLockUntil());
  }

  @Test
  void changePassword_Success() {
    ChangePwRequestDTO request =
        new ChangePwRequestDTO("test@example.com", "oldPassword", "newPassword");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com"), "limit", 1),
            User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.matches("newPassword", "encodedPassword")).thenReturn(false);
    when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
    when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
    when(dbService.update("users", "user-1", user)).thenReturn("2-rev");

    String result = authService.changePassword(request);

    assertEquals("newEncodedPassword", user.getPassword());
    assertFalse(user.getChangePassword());
    assertFalse(user.getFirstLogin());
    assertEquals("2-rev", result);
  }

  @Test
  void changePassword_SameAsOld_ThrowsConflict() {
    ChangePwRequestDTO request =
        new ChangePwRequestDTO("test@example.com", "oldPassword", "oldPassword");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com"), "limit", 1),
            User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

    assertThrows(ConflictException.class, () -> authService.changePassword(request));
  }

  @Test
  void changePassword_WrongOldPassword_ThrowsInvalidCredentials() {
    ChangePwRequestDTO request =
        new ChangePwRequestDTO("test@example.com", "wrongOld", "newPassword");
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("email", "test@example.com"), "limit", 1),
            User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.matches("newPassword", "encodedPassword")).thenReturn(false);
    when(passwordEncoder.matches("wrongOld", "encodedPassword")).thenReturn(false);

    assertThrows(InvalidCredentialsException.class, () -> authService.changePassword(request));
  }

  @Test
  void autoLogin_Success() {
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("userId", "auth-user-1", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of(user));

    AutoLoginResponseDTO result = authService.autoLogin("auth-user-1");

    assertEquals("test@example.com", result.email());
    assertFalse(result.changePassword());
    assertFalse(result.firstLogin());
  }

  @Test
  void autoLogin_NullId_ThrowsInvalidCredentials() {
    assertThrows(InvalidCredentialsException.class, () -> authService.autoLogin(null));
  }

  @Test
  void autoLogin_BlankId_ThrowsInvalidCredentials() {
    assertThrows(InvalidCredentialsException.class, () -> authService.autoLogin(""));
  }

  @Test
  void autoLogin_UserNotFound_ThrowsInvalidCredentials() {
    when(dbService.findByQuery(
            "users",
            Map.of("selector", Map.of("userId", "non-existent", "userActive", true), "limit", 1),
            User.class))
        .thenReturn(List.of());

    assertThrows(InvalidCredentialsException.class, () -> authService.autoLogin("non-existent"));
  }

  @Test
  void generatePassword_Success() {
    String password = AuthService.generatePassword(3, 2);

    assertNotNull(password);
    assertFalse(password.isEmpty());
  }

  @Test
  void generatePassword_TooManyWords_ThrowsIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> AuthService.generatePassword(10000, 2));
  }

  @Test
  void generatePassword_TooManyNumbers_ThrowsIndexOutOfBounds() {
    assertThrows(IndexOutOfBoundsException.class, () -> AuthService.generatePassword(3, 20));
  }
}
