package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gvw.gvwbackend.mapper.UserMapper;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
  @Mock private DbService dbService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private MailService mailService;
  @Mock private SseService sseService;

  private UserService userService;

  @BeforeEach
  void setup() {
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    userService = new UserService(dbService, passwordEncoder, mailService, sseService, userMapper);
  }

  @Test
  void testResetPasswordShouldUpdateAndSetFlag() {
    User user = new User();
    user.setId("321");
    user.setRev("1-rev");
    user.setEmail("test@mail.com");
    user.setUserId("123");
    user.setRole(Role.BOARD_MEMBER);

    when(dbService.findByQuery(any(), any(), eq(User.class)))
        .thenReturn(List.of(user))
        .thenReturn(List.of(user));

    when(passwordEncoder.encode(any())).thenReturn("hashedPw");

    when(dbService.update(eq("users"), eq("321"), any(User.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    userService.resetPasswordUsingMemberId("123");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

    verify(dbService).update(eq("users"), eq("321"), captor.capture());

    User updatedUser = captor.getValue();

    assertEquals("hashedPw", updatedUser.getPassword());
    assertTrue(updatedUser.getChangePassword());

    verify(mailService)
        .sendMail(
            eq("test@mail.com"),
            contains("GVW-Office: Passwort zurückgesetzt"),
            eq("resetPassword"),
            argThat(vars -> vars.containsKey("tempPassword")));
  }
}
