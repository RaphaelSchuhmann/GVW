package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddMemberRequestDTO;
import com.gvw.gvwbackend.dto.request.AddUserRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateUserAdminRequestDTO;
import com.gvw.gvwbackend.dto.response.UserManagerResponseDTO;
import com.gvw.gvwbackend.dto.response.UserResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.mapper.UserMapper;
import com.gvw.gvwbackend.model.Member;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private DbService dbService;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private MailService mailService;

  @Mock private SseService sseService;

  @Mock private UserMapper userMapper;

  @InjectMocks private UserService userService;

  private User user;
  private Member member;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId("user-1");
    user.setUserId("auth-user-1");
    user.setEmail("test@example.com");
    user.setName("John Doe");
    user.setPassword("encodedPassword");
    user.setRole(Role.MEMBER);
    user.setUserActive(true);
    user.setChangePassword(false);
    user.setFirstLogin(false);
    user.setPhone("123456789");
    user.setAddress("123 Street");
    user.setMemberId("member-1");

    member = new Member();
    member.setId("member-1");
    member.setName("John");
    member.setSurname("Doe");
  }

  @Test
  void getUser_Success() {
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("userId", "auth-user-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));

    UserResponseDTO result = userService.getUser("auth-user-1");

    assertEquals("test@example.com", result.email());
    assertEquals("member", result.role());
    assertEquals("John Doe", result.name());
  }

  @Test
  void getUser_NullId_ThrowsInvalidCredentials() {
    assertThrows(InvalidCredentialsException.class, () -> userService.getUser(null));
  }

  @Test
  void getUser_EmptyId_ThrowsInvalidCredentials() {
    assertThrows(InvalidCredentialsException.class, () -> userService.getUser(""));
  }

  @Test
  void getUser_NotFound_ThrowsInvalidCredentials() {
    assertThrows(InvalidCredentialsException.class, () -> userService.getUser(""));
  }

  @Test
  void getUsers_Success() {
    when(dbService.findAll("users", User.class)).thenReturn(List.of(user));

    when(dbService.findByQuery(
            "members",
            Map.of("selector", Map.of("_id", Map.of("$in", Set.of("member-1")))),
            Member.class))
        .thenReturn(List.of(member));

    List<UserManagerResponseDTO> result = userService.getUsers();

    assertEquals(1, result.size());
    assertEquals("user-1", result.getFirst().id());
  }

  @Test
  void getUsers_OrphanedUser_MarksAsOrphaned() {
    user.setMemberId("non-existent-member");

    when(dbService.findAll("users", User.class)).thenReturn(List.of(user));

    when(dbService.findByQuery(
            "members",
            Map.of("selector", Map.of("_id", Map.of("$in", Set.of("non-existent-member")))),
            Member.class))
        .thenReturn(List.of());

    List<UserManagerResponseDTO> result = userService.getUsers();

    assertEquals(1, result.size());
    assertTrue(result.getFirst().isOrphan());
  }

  @Test
  void addOrphanedUser_Success() {
    AddUserRequestDTO request =
        new AddUserRequestDTO("Jane Doe", "jane@example.com", "987654321", "456 Avenue", "MEMBER");
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("email", "jane@example.com")), User.class))
        .thenReturn(List.of());

    userService.addOrphanedUser(request);

    verify(dbService).insert(eq("users"), any(User.class));
    verify(mailService).sendMail(anyString(), anyString(), anyString(), anyMap());
    verify(sseService).sendRefresh("USER");
  }

  @Test
  void addOrphanedUser_EmailExists_ThrowsConflict() {
    AddUserRequestDTO request =
        new AddUserRequestDTO("Jane Doe", "test@example.com", "987654321", "456 Avenue", "MEMBER");
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("email", "test@example.com")), User.class))
        .thenReturn(List.of(user));

    assertThrows(ConflictException.class, () -> userService.addOrphanedUser(request));
  }

  @Test
  void addLinkedUser_Success() {
    AddMemberRequestDTO memberRequest =
        new AddMemberRequestDTO(
            "Jane",
            "Doe",
            "jane@example.com",
            "987654321",
            "456 Avenue",
            "Alto",
            "active",
            "MEMBER",
            "1995-01-01",
            "2021-01-01",
            false,
            "");
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("email", "jane@example.com")), User.class))
        .thenReturn(List.of());

    userService.addLinkedUser(memberRequest, "member-1");

    verify(dbService).insert(eq("users"), any(User.class));
    verify(mailService).sendMail(anyString(), anyString(), anyString(), anyMap());
    verify(sseService).sendRefresh("USER");
  }

  @Test
  void addLinkedUser_NullMemberId_ThrowsIllegalArgumentException() {
    AddMemberRequestDTO memberRequest =
        new AddMemberRequestDTO(
            "Jane",
            "Doe",
            "jane@example.com",
            "987654321",
            "456 Avenue",
            "Alto",
            "active",
            "MEMBER",
            "1995-01-01",
            "2021-01-01",
            false,
            "");

    assertThrows(
        IllegalArgumentException.class, () -> userService.addLinkedUser(memberRequest, null));
  }

  @Test
  void resetPasswordUsingId_Success() {
    when(dbService.findById("users", "user-1", User.class)).thenReturn(user);
    when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-password");
    when(dbService.update("users", "user-1", user)).thenReturn("2-def");

    String result = userService.resetPasswordUsingId("user-1");

    assertEquals("2-def", result);
    assertTrue(user.getChangePassword());
    verify(mailService).sendMail(anyString(), anyString(), anyString(), anyMap());
  }

  @Test
  void resetPasswordUsingId_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> userService.resetPasswordUsingId(null));
  }

  @Test
  void resetPasswordUsingId_NotFound_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> userService.resetPasswordUsingId(""));
  }

  @Test
  void resetPasswordUsingMemberId_Success() {
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("memberId", "member-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));
    when(passwordEncoder.encode(anyString())).thenReturn("new-encoded-password");
    when(dbService.update("users", "user-1", user)).thenReturn("2-def");

    String result = userService.resetPasswordUsingMemberId("member-1");

    assertEquals("2-def", result);
    verify(mailService).sendMail(anyString(), anyString(), anyString(), anyMap());
  }

  @Test
  void resetPasswordUsingMemberId_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> userService.resetPasswordUsingMemberId(null));
  }

  @Test
  void updateUser_Success() {
    UpdateUserAdminRequestDTO request =
        new UpdateUserAdminRequestDTO(
            "user-1",
            "1-abc",
            "Updated Name",
            "new@example.com",
            "987654321",
            "456 Avenue",
            "MEMBER");
    when(dbService.findById("users", "user-1", User.class)).thenReturn(user);
    when(dbService.findByQuery(
            "members", Map.of("selector", Map.of("_id", "member-1")), Member.class))
        .thenReturn(List.of());
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("email", "new@example.com")), User.class))
        .thenReturn(List.of(user));
    when(dbService.update("users", "user-1", user)).thenReturn("2-def");

    String result = userService.updateUser(request);

    assertEquals("2-def", result);
    verify(sseService).sendRefresh("USER");
  }

  @Test
  void updateUser_NotOrphaned_ThrowsBadRequest() {
    UpdateUserAdminRequestDTO request =
        new UpdateUserAdminRequestDTO(
            "user-1",
            "1-abc",
            "Updated Name",
            "new@example.com",
            "987654321",
            "456 Avenue",
            "MEMBER");
    when(dbService.findById("users", "user-1", User.class)).thenReturn(user);
    when(dbService.findByQuery(
            "members", Map.of("selector", Map.of("_id", "member-1")), Member.class))
        .thenReturn(List.of(member));

    assertThrows(BadRequestException.class, () -> userService.updateUser(request));
  }

  @Test
  void deleteUser_Success() {
    when(dbService.findById("users", "user-1", User.class)).thenReturn(user);
    when(dbService.findByQuery(
            "members", Map.of("selector", Map.of("_id", "member-1")), Member.class))
        .thenReturn(List.of());

    userService.deleteUser("user-1");

    verify(dbService).delete("users", "user-1", user.getRev());
    verify(sseService).sendRefresh("USER");
  }

  @Test
  void deleteUser_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> userService.deleteUser(null));
  }

  @Test
  void deleteUser_NotOrphaned_ThrowsBadRequest() {
    when(dbService.findById("users", "user-1", User.class)).thenReturn(user);
    when(dbService.findByQuery(
            "members", Map.of("selector", Map.of("_id", "member-1")), Member.class))
        .thenReturn(List.of(member));

    assertThrows(BadRequestException.class, () -> userService.deleteUser("user-1"));
  }

  @Test
  void resolveUserIdToEmail_Success() {
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("userId", "auth-user-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));

    String result = userService.resolveUserIdToEmail("auth-user-1");

    assertEquals("test@example.com", result);
  }

  @Test
  void resolveUserIdToEmail_NullId_ReturnsEmptyString() {
    String result = userService.resolveUserIdToEmail(null);

    assertEquals("", result);
  }

  @Test
  void resolveUserIdToEmail_NotFound_ReturnsEmptyString() {
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("userId", "non-existent"), "limit", 1), User.class))
        .thenReturn(List.of());

    String result = userService.resolveUserIdToEmail("non-existent");

    assertEquals("", result);
  }

  @Test
  void getUserByUserId_Success() {
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("userId", "auth-user-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));

    User result = userService.getUserByUserId("auth-user-1", ErrorAction.READ_ONE);

    assertEquals("user-1", result.getId());
  }

  @Test
  void getUserByUserId_NotFound_ThrowsNotFound() {
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("userId", "non-existent"), "limit", 1), User.class))
        .thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> userService.getUserByUserId("non-existent", ErrorAction.READ_ONE));
  }
}
