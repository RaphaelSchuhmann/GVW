package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.gvw.gvwbackend.dto.request.AddMemberRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateMemberRequestDTO;
import com.gvw.gvwbackend.exception.BadRequestException;
import com.gvw.gvwbackend.exception.ConflictException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.mapper.MemberMapper;
import com.gvw.gvwbackend.model.Member;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {
  @Mock private DbService dbService;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private MailService mailService;
  @Mock private SseService sseService;

  private MemberService memberService;

  @BeforeEach
  void setup() {
    MemberMapper memberMapper = Mappers.getMapper(MemberMapper.class);
    memberService =
        new MemberService(dbService, memberMapper, passwordEncoder, mailService, sseService);
  }

  @Test
  void testAddMemberShouldCreateMemberAndUser() {
    AddMemberRequestDTO request =
        new AddMemberRequestDTO(
            "Max",
            "Mustermann",
            "test@mail.com",
            "phoneNumber",
            "address",
            "t1",
            "member",
            "active",
            "birthdate",
            "joined");

    Member savedMember = generateValidMember();
    savedMember.setId("member-id");

    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(List.of());

    when(dbService.findByQuery(eq("members"), any(), eq(Member.class)))
        .thenReturn(List.of(savedMember));

    memberService.addMember(request);

    verify(dbService).insert(eq("members"), any(Member.class));
    verify(dbService)
        .insert(
            eq("users"),
            argThat(
                (User user) ->
                    user.getMemberId().equals("member-id")
                        && user.getEmail().equals("test@mail.com")));

    verify(mailService)
        .sendMail(
            eq("test@mail.com"),
            contains("GVW-Office: Temporäres Password"),
            eq("newUser"),
            argThat(vars -> vars.containsKey("tempPassword")));
  }

  @Test
  void testAddMemberShouldThrowConflictExceptionWhenEmailExists() {
    AddMemberRequestDTO request =
        new AddMemberRequestDTO(
            "Max",
            "Mustermann",
            "test@mail.com",
            "phoneNumber",
            "address",
            "t1",
            "member",
            "active",
            "birthdate",
            "joined");

    when(dbService.findByQuery(eq("users"), any(), eq(User.class)))
        .thenReturn(List.of(generateValidUser()));

    assertThrows(
        ConflictException.class,
        () -> {
          memberService.addMember(request);
        });

    verify(dbService, never()).insert(eq("members"), any());
  }

  @Test
  void testAddMemberShouldRollbackWhenUserInsertFails() {
    AddMemberRequestDTO request =
        new AddMemberRequestDTO(
            "Max",
            "Mustermann",
            "test@mail.com",
            "phoneNumber",
            "address",
            "t1",
            "member",
            "active",
            "birthdate",
            "joined");

    Member savedMember = generateValidMember();
    savedMember.setId("member-id");
    savedMember.setRev("rev-1");

    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(List.of());

    when(dbService.findByQuery(eq("members"), any(), eq(Member.class)))
        .thenReturn(List.of(savedMember));

    when(dbService.insert(eq("members"), any(Member.class))).thenReturn(true);

    when(dbService.insert(eq("users"), any(User.class)))
        .thenThrow(new RuntimeException("DB error"));

    assertThrows(RuntimeException.class, () -> memberService.addMember(request));

    verify(dbService).delete("members", "member-id", "rev-1");

    verify(mailService, never()).sendMail(anyString(), anyString(), anyString(), anyMap());
  }

  @Test
  void testAddMemberShouldThrowNotFoundWhenMemberNotFoundAfterInsert() {
    AddMemberRequestDTO request =
        new AddMemberRequestDTO(
            "Max",
            "Mustermann",
            "test@mail.com",
            "phoneNumber",
            "address",
            "t1",
            "member",
            "active",
            "birthdate",
            "joined");

    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(List.of());

    when(dbService.findByQuery(eq("members"), any(), eq(Member.class))).thenReturn(List.of());

    assertThrows(
        RuntimeException.class,
        () -> {
          memberService.addMember(request);
        });
  }

  @Test
  void testAddMemberShouldSendCorrectPassword() {
    AddMemberRequestDTO request =
        new AddMemberRequestDTO(
            "Max",
            "Mustermann",
            "test@mail.com",
            "phoneNumber",
            "address",
            "t1",
            "member",
            "active",
            "birthdate",
            "joined");

    Member savedMember = generateValidMember();
    savedMember.setId("member-id");

    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(List.of());

    when(dbService.findByQuery(eq("members"), any(), eq(Member.class)))
        .thenReturn(List.of(savedMember));

    memberService.addMember(request);

    ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(mailService).sendMail(anyString(), anyString(), anyString(), mapCaptor.capture());

    Map<String, Object> sentVariables = mapCaptor.getValue();
    String password = (String) sentVariables.get("tempPassword");

    assertNotNull(password);
  }

  @Test
  void testDeleteMemberShouldDeleteMemberAndUser() {
    Member savedMember = generateValidMember();
    User savedUser = generateValidUser();

    savedMember.setId("id-123");
    savedMember.setRev("rev-1");

    savedUser.setId("id-123");
    savedUser.setMemberId(savedMember.getId());
    savedUser.setRev(savedMember.getRev());

    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(savedMember);
    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(List.of(savedUser));

    memberService.deleteMember(savedMember.getId());

    verify(dbService).delete("members", "id-123", "rev-1");
    verify(dbService).delete("users", "id-123", "rev-1");
  }

  @Test
  void testDeleteMemberShouldThrowBadRequestWhenIdIsNullOrEmpty() {
    assertThrows(
        BadRequestException.class,
        () -> {
          memberService.deleteMember("");
        });

    assertThrows(
        BadRequestException.class,
        () -> {
          memberService.deleteMember(null);
        });
  }

  @Test
  void testDeleteMemberShouldThrowNotFoundWhenMemberIsNotFound() {
    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(null);

    assertThrows(
        NotFoundException.class,
        () -> {
          memberService.deleteMember("member-id");
        });
  }

  @Test
  void testUpdateMemberShouldUpdateMemberAndUser() {
    String memberId = "member-id";

    UpdateMemberRequestDTO request =
        new UpdateMemberRequestDTO(
            memberId,
            "Max",
            "Mustermann",
            "new@mail.com",
            "phone",
            "address",
            "t1",
            "active",
            "member",
            "birthdate",
            "joined",
            "1-rev");

    Member existingMember = generateValidMember();
    existingMember.setId(memberId);
    existingMember.setRev("rev-member");

    User existingUser = generateValidUser();
    existingUser.setId("user-id");
    existingUser.setRev("rev-user");
    existingUser.setMemberId(memberId);

    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(existingMember);
    when(dbService.findByQuery(eq("users"), any(), eq(User.class)))
        .thenReturn(List.of(existingUser));

    when(dbService.update(eq("members"), eq("member-id"), any(Member.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));
    when(dbService.update(eq("users"), eq("user-id"), any(User.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    List<String> resp = memberService.updateMember(request);

    assertNotNull(resp);
    assertFalse(resp.isEmpty());
    assertFalse(resp.getFirst().isBlank());
    assertFalse(resp.get(1).isBlank());

    verify(dbService).update("members", existingMember.getId(), existingMember);
    verify(dbService).update("users", existingUser.getId(), existingUser);

    assertEquals("Max", existingMember.getName());
    assertEquals("Mustermann", existingMember.getSurname());
    assertEquals("new@mail.com", existingMember.getEmail());

    assertEquals("Max Mustermann", existingUser.getName());
    assertEquals("new@mail.com", existingUser.getEmail());

    assertEquals("user-id", existingUser.getId());
    assertEquals("rev-user", existingUser.getRev());

    assertEquals(memberId, existingMember.getId());
    assertEquals("rev-member", existingMember.getRev());
  }

  @Test
  void testUpdateMemberShouldThrowNotFoundWhenUserNotFound() {
    Member savedMember = generateValidMember();
    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(savedMember);
    when(dbService.findByQuery(eq("users"), any(), eq(User.class))).thenReturn(null);

    UpdateMemberRequestDTO request =
        new UpdateMemberRequestDTO(
            "member-id",
            "Max",
            "Mustermann",
            "new@mail.com",
            "phone",
            "address",
            "t1",
            "active",
            "role",
            "birthdate",
            "joined",
            "1-rev");

    assertThrows(
        NotFoundException.class,
        () -> {
          memberService.updateMember(request);
        });
  }

  @Test
  void testUpdateMemberShouldThrowNotFoundWhenMemberNotFound() {
    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(null);

    UpdateMemberRequestDTO request =
        new UpdateMemberRequestDTO(
            "member-id",
            "Max",
            "Mustermann",
            "new@mail.com",
            "phone",
            "address",
            "t1",
            "active",
            "role",
            "birthdate",
            "joined",
            "1-rev");

    assertThrows(
        NotFoundException.class,
        () -> {
          memberService.updateMember(request);
        });
  }

  @Test
  void testUpdateMemberStatusShouldUpdateMemberStatusFromActiveToInactive() {
    Member savedMemberActive = generateValidMember();
    savedMemberActive.setId("member-id");
    savedMemberActive.setRev("1-rev");

    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(savedMemberActive);
    when(dbService.update(eq("members"), eq("member-id"), any(Member.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    String resp = memberService.updateMemberStatus("member-id", "1-rev");

    assertNotNull(resp);
    assertFalse(resp.isBlank());

    verify(dbService).update("members", savedMemberActive.getId(), savedMemberActive);

    assertEquals("inactive", savedMemberActive.getStatus());
  }

  @Test
  void testUpdateMemberStatusShouldUpdateMemberStatusFromInactiveToActive() {
    Member savedMemberActive = generateValidMember();
    savedMemberActive.setId("member-id");
    savedMemberActive.setStatus("inactive");
    savedMemberActive.setRev("1-rev");

    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(savedMemberActive);
    when(dbService.update(eq("members"), eq("member-id"), any(Member.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    String resp = memberService.updateMemberStatus("member-id", "1-rev");

    assertNotNull(resp);
    assertFalse(resp.isBlank());

    verify(dbService).update("members", savedMemberActive.getId(), savedMemberActive);

    assertEquals("active", savedMemberActive.getStatus());
  }

  @Test
  void testUpdateMemberStatusShouldUpdateMemberStatusFromNullToActive() {
    Member savedMemberActive = generateValidMember();
    savedMemberActive.setId("member-id");
    savedMemberActive.setStatus(null);
    savedMemberActive.setRev("1-rev");

    when(dbService.findById(eq("members"), any(), eq(Member.class))).thenReturn(savedMemberActive);
    when(dbService.update(eq("members"), eq("member-id"), any(Member.class)))
        .thenReturn(Map.of("ok", true, "rev", "2-newrev"));

    String resp = memberService.updateMemberStatus("member-id", "1-rev");

    assertNotNull(resp);
    assertFalse(resp.isBlank());

    verify(dbService).update("members", savedMemberActive.getId(), savedMemberActive);

    assertEquals("active", savedMemberActive.getStatus());
  }

  private Member generateValidMember() {
    Member member = new Member();
    member.setName("Max");
    member.setSurname("Mustermann");
    member.setEmail("test@mail.com");
    member.setPhone("phoneNumber");
    member.setAddress("address");
    member.setVoice("t1");
    member.setRole(Role.MEMBER);
    member.setStatus("active");
    member.setBirthdate("birthdate");
    member.setJoined("joined");

    return member;
  }

  private User generateValidUser() {
    User user = new User();
    user.setEmail("test@mail.com");
    user.setName("Max Mustermann");
    user.setPhone("phoneNumber");
    user.setAddress("address");
    user.setChangePassword(true);
    user.setFirstLogin(true);
    user.setUserId(UUID.randomUUID().toString());
    user.setRole(Role.MEMBER);
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);

    return user;
  }
}
