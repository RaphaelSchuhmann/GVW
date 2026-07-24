package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gvw.gvwbackend.dto.request.ChangePwRequestDTO;
import com.gvw.gvwbackend.dto.request.LoginRequestDTO;
import com.gvw.gvwbackend.dto.response.LoginResponseDTO;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.InvalidCredentialsException;
import com.gvw.gvwbackend.exception.TooManyRequestsException;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

  @Mock private DbService dbService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  @InjectMocks private AuthService authService;

  @Test
  void testLoginShouldSucceedWhenValidCredentials() {
    User user = new User();
    user.setUserId("123");
    user.setId("123");
    user.setRev("1-rev");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setRole(Role.MEMBER);
    user.setFailedLoginAttempts(2);
    user.setLockUntil(null);
    user.setChangePassword(false);
    user.setFirstLogin(false);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));

    when(passwordEncoder.matches("plainPw", "hashedPw")).thenReturn(true);
    when(dbService.update(eq("users"), eq("123"), any(User.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    when(jwtService.generateToken(eq("123"), anyMap())).thenReturn("mocked-jwt");

    LoginRequestDTO request = new LoginRequestDTO("test@mail.com", "plainPw");

    LoginResponseDTO response = authService.login(request);

    assertEquals("mocked-jwt", response.authToken());
    assertFalse(response.changePassword());
    assertFalse(response.firstLogin());

    verify(dbService)
        .update(
            eq("users"),
            eq("123"),
            argThat(
                (User updatedUser) ->
                    updatedUser.getFailedLoginAttempts() == 0
                        && updatedUser.getLockUntil() == null));

    verify(jwtService).generateToken(eq("123"), anyMap());
  }

  @Test
  void testLoginShouldUnauthorizedIfWrongEmail() {
    User user = new User();
    user.setUserId("123");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(2);
    user.setLockUntil(null);
    user.setChangePassword(false);
    user.setFirstLogin(false);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of());

    LoginRequestDTO request = new LoginRequestDTO("test@a.com", "plainPw");

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.login(request);
        });
  }

  @Test
  void testLoginShouldToManyRequestsIfUserLocked() {
    User user = new User();
    user.setUserId("123");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(5);
    user.setLockUntil(Instant.now().plus(Duration.ofMinutes(15)));
    user.setChangePassword(false);
    user.setFirstLogin(false);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));

    LoginRequestDTO request = new LoginRequestDTO("test@mail.com", "plainPw");

    assertThrows(
        TooManyRequestsException.class,
        () -> {
          authService.login(request);
        });
  }

  @Test
  void testLoginShouldLockAccountWhenTooManyFailedAttempts() {
    User user = new User();
    user.setId("123");
    user.setRev("1-rev");
    user.setUserId("123");
    user.setEmail("mail");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(5);
    user.setLockUntil(null);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));
    when(dbService.update(eq("users"), eq("123"), any(User.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    LoginRequestDTO request = new LoginRequestDTO("mail", "wrongPw");

    TooManyRequestsException ex =
        assertThrows(TooManyRequestsException.class, () -> authService.login(request));

    verify(dbService)
        .update(
            eq("users"),
            eq("123"),
            argThat((User updatedUser) -> updatedUser.getLockUntil() != null));
  }

  @Test
  void testLoginShouldIncreaseFailedAttemptsWhenInvalidPw() {
    User user = new User();
    user.setUserId("123");
    user.setId("123");
    user.setRev("1-rev");
    user.setEmail("mail");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(2);
    user.setLockUntil(null);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));
    when(dbService.update(eq("users"), eq("123"), any(User.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    when(passwordEncoder.matches(any(), any())).thenReturn(false);

    LoginRequestDTO request = new LoginRequestDTO("mail", "wrongPw");

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.login(request);
        });

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

    verify(dbService).update(eq("users"), eq("123"), captor.capture());

    User updatedUser = captor.getValue();

    assertEquals(3, updatedUser.getFailedLoginAttempts());
  }

  @Test
  void testChangePasswordShouldChangeUserPassword() {
    User user = new User();
    user.setUserId("123");
    user.setId("123");
    user.setRev("1-rev");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);
    user.setChangePassword(true);
    user.setFirstLogin(true);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));
    when(dbService.update(eq("users"), eq("123"), any(User.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    when(passwordEncoder.matches(any(), any())).thenReturn(false).thenReturn(true);

    when(passwordEncoder.encode("newPw")).thenReturn("hashedNewPw");

    ChangePwRequestDTO request = new ChangePwRequestDTO("test@mail.com", "oldPw", "newPw");

    authService.changePassword(request);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

    verify(dbService).update(eq("users"), eq("123"), captor.capture());

    User updatedUser = captor.getValue();

    assertEquals("hashedNewPw", updatedUser.getPassword());
    assertFalse(updatedUser.getChangePassword());
    assertFalse(updatedUser.getFirstLogin());
  }

  @Test
  void testChangePasswordShouldFailIfInvalidEmail() {
    User user = new User();
    user.setUserId("123");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);
    user.setChangePassword(true);
    user.setFirstLogin(true);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of());

    ChangePwRequestDTO request = new ChangePwRequestDTO("test2@mail.com", "oldPw", "newPw");

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.changePassword(request);
        });
  }

  @Test
  void testChangePasswordShouldFailIfNewPwIsSameAsOld() {
    User user = new User();
    user.setUserId("123");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);
    user.setChangePassword(true);
    user.setFirstLogin(true);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));

    when(passwordEncoder.matches(any(), any())).thenReturn(true);

    ChangePwRequestDTO request = new ChangePwRequestDTO("test@mail.com", "oldPw", "oldPw");

    assertThrows(
        ConflictException.class,
        () -> {
          authService.changePassword(request);
        });
  }

  @Test
  void testChangePasswordShouldFailIfOldPwIsInvalid() {
    User user = new User();
    user.setUserId("123");
    user.setEmail("test@mail.com");
    user.setPassword("hashedPw");
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);
    user.setChangePassword(true);
    user.setFirstLogin(true);

    when(dbService.findByQuery(any(), any(), eq(User.class))).thenReturn(List.of(user));

    when(passwordEncoder.matches(any(), any())).thenReturn(false).thenReturn(false);

    ChangePwRequestDTO request = new ChangePwRequestDTO("test@mail.com", "invalidOldPw", "newPw");

    assertThrows(
        InvalidCredentialsException.class,
        () -> {
          authService.changePassword(request);
        });
  }

  @Test
  void testGeneratePasswordShouldContainWordsAndDigits() {
    String result = AuthService.generatePassword(3, 2);

    long digitCount = result.chars().filter(Character::isDigit).count();

    long upperCaseCount = result.chars().filter(Character::isUpperCase).count();

    assertEquals(2, digitCount);
    assertEquals(3, upperCaseCount);
  }

  @Test
  void testGeneratePasswordShouldReturnEmptyWhenZeroInputs() {
    String result = AuthService.generatePassword(0, 0);

    assertEquals("", result);
  }

  @Test
  void testGeneratePasswordShouldContainOnlyWords() {
    String result = AuthService.generatePassword(3, 0);

    long digitCount = result.chars().filter(Character::isDigit).count();

    assertEquals(0, digitCount);
  }

  @Test
  void testGeneratePasswordShouldContainOnlyDigits() {
    String result = AuthService.generatePassword(0, 5);

    long digitCount = result.chars().filter(Character::isDigit).count();

    assertEquals(5, digitCount);
  }
}
