package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddUserAdminRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateUserAdminRequestDTO;
import com.gvw.gvwbackend.dto.response.UserManagerResponseDTO;
import com.gvw.gvwbackend.dto.response.UserManagerResponsesDTO;
import com.gvw.gvwbackend.dto.response.UserResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.mapper.UserMapper;
import com.gvw.gvwbackend.model.Member;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class UserService {
  private final DbService dbService;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final ObjectMapper mapper = new ObjectMapper();
  private final SseService sseService;
  private final UserMapper userMapper;
  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  public UserService(
      DbService dbService,
      PasswordEncoder passwordEncoder,
      MailService mailService,
      SseService sseService,
      UserMapper userMapper) {
    this.dbService = dbService;
    this.passwordEncoder = passwordEncoder;
    this.mailService = mailService;
    this.sseService = sseService;
    this.userMapper = userMapper;
  }

  public UserResponseDTO getUser(String userId) {
    if (userId == null || userId.isEmpty()) {
      // This is invalid credentials aka invalid token so logout should be handled via 1004401 (Auth
      // Middleware error)
      throw new InvalidCredentialsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401)));
    }

    User user = getUserByUserId(userId, ErrorAction.READ_ONE);

    return new UserResponseDTO(
        user.getEmail(),
        user.getRole().getValue(),
        user.getName(),
        user.getAddress(),
        user.getPhone(),
        user.getRev());
  }

  public String resetPasswordUsingMemberId(String memberId) {
    if (memberId == null || memberId.isEmpty()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 400)));
    }

    User user = getUserByMemberId(memberId, ErrorAction.UPDATE);

    String temporaryPassword = AuthService.generatePassword(3, 2);

    user.setPassword(passwordEncoder.encode(temporaryPassword));
    user.setChangePassword(true);
    Map<String, Object> resp = dbService.update("users", user.getId(), user);

    if (resp == null || !resp.containsKey("rev")) {
      throw new RuntimeException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 500)));
    }

    mailService.sendMail(
        user.getEmail(),
        "GVW-Office: Passwort zurückgesetzt",
        "resetPassword",
        Map.of("tempPassword", temporaryPassword));

    return (String) resp.get("rev");
  }

  public UserManagerResponsesDTO getUsers() {
    List<Map<String, Object>> usersRaw = dbService.findAll("users");
    List<User> users = usersRaw.stream().map(map -> mapper.convertValue(map, User.class)).toList();
    if (users.isEmpty()) return new UserManagerResponsesDTO(List.of());

    // Collect non-empty memberIds
    Set<String> memberIds =
        users.stream()
            .map(User::getMemberId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());

    // One bulk lookup against members
    Set<String> existingMemberIds =
        memberIds.isEmpty()
            ? Set.of()
            : dbService
                .findByQuery(
                    "members",
                    Map.of("selector", Map.of("_id", Map.of("$in", memberIds))),
                    Member.class)
                .stream()
                .map(Member::getId)
                .collect(Collectors.toSet());

    List<UserManagerResponseDTO> dtos =
        users.stream()
            .map(
                m ->
                    new UserManagerResponseDTO(
                        m.getId(),
                        m.getRev(),
                        m.getName(),
                        m.getEmail(),
                        m.getPhone(),
                        m.getAddress(),
                        m.getRole().getValue(),
                        m.getMemberId() == null
                            || m.getMemberId().isBlank()
                            || !existingMemberIds.contains(m.getMemberId())))
            .toList();

    return new UserManagerResponsesDTO(dtos);
  }

  public void checkUser(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.CHECK, 400)));
    }

    getUserByID(id, ErrorAction.CHECK);
  }

  public void addUser(AddUserAdminRequestDTO request) {
    List<User> usersWithRequestMail =
        dbService.findByQuery(
            "users", Map.of("selector", Map.of("email", request.email())), User.class);

    if (!usersWithRequestMail.isEmpty()) {
      throw new ConflictException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.CREATE, 409)));
    }

    User user = createUserFromRequest(request);

    String temporaryPassword = AuthService.generatePassword(3, 2);

    user.setPassword(passwordEncoder.encode(temporaryPassword));

    dbService.insert("users", user);

    mailService.sendMail(
        user.getEmail(),
        "GVW-Office: Temporäres Password",
        "newUser",
        Map.of("tempPassword", temporaryPassword));

    try {
      sseService.broadcastRefresh("USER");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast USER refresh: ", ex);
    }
  }

  public String resetPasswordUsingUserId(String id) {
    if (id == null || id.isEmpty()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 400)));
    }

    User user = getUserByID(id, ErrorAction.UPDATE);

    String temporaryPassword = AuthService.generatePassword(3, 2);

    user.setPassword(passwordEncoder.encode(temporaryPassword));
    user.setChangePassword(true);
    Map<String, Object> resp = dbService.update("users", user.getId(), user);

    if (resp == null || !resp.containsKey("rev")) {
      throw new RuntimeException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 500)));
    }

    mailService.sendMail(
        user.getEmail(),
        "GVW-Office: Passwort zurückgesetzt",
        "resetPassword",
        Map.of("tempPassword", temporaryPassword));

    return (String) resp.get("rev");
  }

  public String updateUser(UpdateUserAdminRequestDTO request) {
    User user = getUserByID(request.id(), ErrorAction.UPDATE);

    if (!isOrphan(user.getMemberId())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 400)));
    }

    if (!user.getEmail().equalsIgnoreCase(request.email())) {
      List<User> conflicts =
          dbService.findByQuery(
              "users", Map.of("selector", Map.of("email", request.email())), User.class);
      if (conflicts.stream().anyMatch(u -> !u.getId().equals(user.getId()))) {
        throw new ConflictException(
            String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 409)));
      }
    }

    userMapper.updateUserFromDto(request, user);

    user.setRev(request.rev());

    Map<String, Object> userResult = dbService.update("users", user.getId(), user);

    try {
      sseService.broadcastRefresh("USER");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast USER refresh: ", ex);
    }

    if (userResult != null && userResult.containsKey("rev")) {
      return (String) userResult.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.USER.createCode(ErrorAction.UPDATE, 500)));
  }

  public void deleteUser(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.DELETE, 400)));
    }

    User user = getUserByID(id, ErrorAction.DELETE);

    if (!isOrphan(user.getMemberId())) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.USER.createCode(ErrorAction.DELETE, 400)));
    }

    dbService.delete("users", user.getId(), user.getRev());

    try {
      sseService.broadcastRefresh("USER");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast USER refresh: ", ex);
    }
  }

  private User getUserByMemberId(String memberId, ErrorAction action) {
    Map<String, Object> query = Map.of("selector", Map.of("memberId", memberId), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);

    if (users == null || users.isEmpty())
      throw new NotFoundException(String.valueOf(ErrorDomain.USER.createCode(action, 404)));

    return users.getFirst();
  }

  private User getUserByUserId(String userId, ErrorAction action) {
    Map<String, Object> query = Map.of("selector", Map.of("userId", userId), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);

    if (users == null || users.isEmpty())
      throw new NotFoundException(String.valueOf(ErrorDomain.USER.createCode(action, 404)));

    return users.getFirst();
  }

  private User getUserByID(String id, ErrorAction action) {
    User user = dbService.findById("users", id, User.class);

    if (user == null)
      throw new NotFoundException(String.valueOf(ErrorDomain.USER.createCode(action, 404)));

    return user;
  }

  public String resolveUserIdToEmail(String id) {
    if (id == null || id.isBlank()) {
      return "";
    }

    List<User> users =
        dbService.findByQuery(
            "users", Map.of("selector", Map.of("userId", id), "limit", 1), User.class);
    if (users == null || users.isEmpty()) {
      return "";
    }

    String email = users.getFirst().getEmail();
    return email == null ? "" : email;
  }

  private boolean isOrphan(String memberId) {
    if (memberId == null || memberId.isBlank()) return true;

    Map<String, Object> query = Map.of("selector", Map.of("_id", memberId));
    List<Member> members = dbService.findByQuery("members", query, Member.class);

    return members == null || members.isEmpty();
  }

  private User createUserFromRequest(AddUserAdminRequestDTO request) {
    User user = new User();
    user.setEmail(request.email());
    user.setName(request.name());
    user.setPhone(request.phone());
    user.setAddress(request.address());
    user.setChangePassword(true);
    user.setFirstLogin(true);
    user.setUserId(UUID.randomUUID().toString());
    user.setRole(Role.fromString(request.role()));
    user.setFailedLoginAttempts(0);
    user.setLockUntil(null);

    return user;
  }
}
