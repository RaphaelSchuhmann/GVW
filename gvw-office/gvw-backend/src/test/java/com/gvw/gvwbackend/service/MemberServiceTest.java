package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddMemberRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateMemberRequestDTO;
import com.gvw.gvwbackend.dto.response.MemberResponseDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.mapper.MemberMapper;
import com.gvw.gvwbackend.model.Member;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

  @Mock private DbService dbService;

  @Mock private MemberMapper memberMapper;

  @Mock private SseService sseService;

  @Mock private UserService userService;

  @InjectMocks private MemberService memberService;

  private Member member;
  private User user;

  @BeforeEach
  void setUp() {
    member = new Member();
    member.setId("member-1");
    member.setRev("1-abc");
    member.setName("John");
    member.setSurname("Doe");
    member.setEmail("john@example.com");
    member.setPhone("123456789");
    member.setAddress("123 Street");
    member.setVoice("Soprano");
    member.setStatus("active");
    member.setRole(Role.MEMBER);
    member.setBirthdate("1990-01-01");
    member.setJoined("2020-01-01");

    user = new User();
    user.setId("user-1");
    user.setUserId("auth-user-1");
    user.setEmail("john@example.com");
    user.setPassword("encodedPassword");
    user.setRole(Role.MEMBER);
    user.setUserActive(true);
    user.setMemberId("member-1");
  }

  @Test
  void getMembers_Success() {
    when(dbService.findAll("members", Member.class)).thenReturn(List.of(member));

    List<MemberResponseDTO> result = memberService.getMembers();

    assertEquals(1, result.size());
    assertEquals("member-1", result.get(0).id());
    assertEquals("John", result.get(0).name());
  }

  @Test
  void getMembers_Empty_ReturnsEmptyList() {
    when(dbService.findAll("members", Member.class)).thenReturn(List.of());

    List<MemberResponseDTO> result = memberService.getMembers();

    assertTrue(result.isEmpty());
  }

  @Test
  void addMember_Success() {
    AddMemberRequestDTO request =
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
            "users",
            Map.of("selector", Map.of("email", "jane@example.com"), "limit", 1),
            User.class))
        .thenReturn(List.of());
    when(dbService.findByQuery(
            "members",
            Map.of("selector", Map.of("email", "jane@example.com"), "limit", 1),
            Member.class))
        .thenReturn(List.of(member));

    memberService.addMember(request);

    verify(dbService).insert(eq("members"), any(Member.class));
    verify(userService).addLinkedUser(eq(request), eq("member-1"));
    verify(sseService).sendRefresh("MEMBERS");
  }

  @Test
  void addMember_EmailExists_ThrowsConflict() {
    AddMemberRequestDTO request =
        new AddMemberRequestDTO(
            "Jane",
            "Doe",
            "john@example.com",
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
            "users",
            Map.of("selector", Map.of("email", "john@example.com"), "limit", 1),
            User.class))
        .thenReturn(List.of(user));

    assertThrows(ConflictException.class, () -> memberService.addMember(request));
  }

  @Test
  void deleteMember_Success() {
    when(dbService.findById("members", "member-1", Member.class)).thenReturn(member);
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("memberId", "member-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));

    memberService.deleteMember("member-1");

    verify(dbService).delete("members", "member-1", "1-abc");
    verify(dbService).delete("users", "user-1", user.getRev());
    verify(sseService).sendRefresh("MEMBERS");
  }

  @Test
  void deleteMember_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> memberService.deleteMember(null));
  }

  @Test
  void deleteMember_NotFound_ThrowsNotFound() {
    when(dbService.findById("members", "non-existent", Member.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> memberService.deleteMember("non-existent"));
  }

  @Test
  void updateMember_Success() {
    UpdateMemberRequestDTO request =
        new UpdateMemberRequestDTO(
            "member-1",
            "John Updated",
            "Doe",
            "john@example.com",
            "123456789",
            "123 Street",
            "Soprano",
            "active",
            "MEMBER",
            "1990-01-01",
            "2020-01-01",
            false,
            "",
            "1-abc");
    when(dbService.findById("members", "member-1", Member.class)).thenReturn(member);
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("memberId", "member-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));
    when(dbService.update("members", "member-1", member)).thenReturn("2-def");
    when(dbService.update("users", "user-1", user)).thenReturn("2-ghi");

    List<String> result = memberService.updateMember(request);

    assertEquals(2, result.size());
    assertEquals("2-def", result.get(0));
    assertEquals("2-ghi", result.get(1));
    verify(sseService).sendRefresh("MEMBERS");
  }

  @Test
  void updateMember_NotFound_ThrowsNotFound() {
    UpdateMemberRequestDTO request =
        new UpdateMemberRequestDTO(
            "non-existent",
            "John",
            "Doe",
            "john@example.com",
            "123456789",
            "123 Street",
            "Soprano",
            "active",
            "MEMBER",
            "1990-01-01",
            "2020-01-01",
            false,
            "",
            "1-abc");
    when(dbService.findById("members", "non-existent", Member.class)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> memberService.updateMember(request));
  }

  @Test
  void updateMemberStatus_Success() {
    when(dbService.findById("members", "member-1", Member.class)).thenReturn(member);
    when(dbService.findByQuery(
            "users", Map.of("selector", Map.of("memberId", "member-1"), "limit", 1), User.class))
        .thenReturn(List.of(user));
    when(dbService.update("members", "member-1", member)).thenReturn("2-def");
    when(dbService.update("users", "user-1", user)).thenReturn("2-ghi");

    List<String> result = memberService.updateMemberStatus("member-1", "1-abc");

    assertEquals(2, result.size());
    assertEquals("inactive", member.getStatus());
    verify(sseService).sendRefresh("MEMBERS");
  }

  @Test
  void updateMemberStatus_NullId_ThrowsBadRequest() {
    assertThrows(BadRequestException.class, () -> memberService.updateMemberStatus(null, "1-abc"));
  }

  @Test
  void updateMemberStatus_NotFound_ThrowsNotFound() {
    when(dbService.findById("members", "non-existent", Member.class)).thenReturn(null);

    assertThrows(
        NotFoundException.class, () -> memberService.updateMemberStatus("non-existent", "1-abc"));
  }
}
