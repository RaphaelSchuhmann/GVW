package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.AddMemberRequestDTO;
import com.gvw.gvwbackend.dto.request.UpdateMemberRequestDTO;
import com.gvw.gvwbackend.dto.response.MemberResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.mapper.MemberMapper;
import com.gvw.gvwbackend.model.Member;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service responsible for managing members and their linked user accounts.
 *
 * <p>Provides functionality for retrieving, creating, updating, and deleting members. Member
 * operations may also create or update associated user accounts to keep member and authentication
 * data synchronized.
 *
 * <p>Uses {@link DbService} for persistence, {@link MemberMapper} for DTO mapping, {@link
 * MailService} for account notifications, and {@link SseService} for broadcasting real-time
 * updates.
 */
@Service
public class MemberService {
  private final DbService dbService;
  private final MemberMapper memberMapper;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final SseService sseService;
  private static final Logger log = LoggerFactory.getLogger(MemberService.class);

  /**
   * Creates a new member service instance.
   *
   * @param dbService service used for database operations
   * @param memberMapper mapper used for updating member and user entities
   * @param passwordEncoder encoder used for storing user passwords securely
   * @param mailService service used for sending account emails
   * @param sseService service used for broadcasting member changes
   */
  public MemberService(
      DbService dbService,
      MemberMapper memberMapper,
      PasswordEncoder passwordEncoder,
      MailService mailService,
      SseService sseService) {
    this.dbService = dbService;
    this.memberMapper = memberMapper;
    this.passwordEncoder = passwordEncoder;
    this.mailService = mailService;
    this.sseService = sseService;
  }

  /**
   * Retrieves all members.
   *
   * <p>Loads members from storage and converts them into response DTOs for administration and
   * display purposes.
   *
   * @return list of all members
   */
  public List<MemberResponseDTO> getMembers() {
    List<Member> members = dbService.findAll("members", Member.class);

    if (members.isEmpty()) {
      return List.of();
    }

    return members.stream()
        .map(
            m ->
                new MemberResponseDTO(
                    m.getId(),
                    m.getRev(),
                    m.getName(),
                    m.getSurname(),
                    m.getEmail(),
                    m.getPhone(),
                    m.getAddress(),
                    m.getVoice(),
                    m.getStatus(),
                    m.getRole().getValue(),
                    m.getBirthdate(),
                    m.getJoined()))
        .toList();
  }

  /**
   * Checks whether a member exists.
   *
   * @param id identifier of the member
   * @throws BadRequestException if the identifier is empty
   * @throws NotFoundException if no member exists with the given identifier
   */
  public void checkMember(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.CHECK, 400)));
    }

    Member member = dbService.findById("members", id, Member.class);
    if (member == null) {
      throw new NotFoundException(
          String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.CHECK, 404)));
    }
  }

  /**
   * Creates a new member and the associated user account.
   *
   * <p>Creates the member record, generates a linked user account with a temporary password, sends
   * the credentials via email, and broadcasts a member refresh event.
   *
   * <p>If user creation fails after the member has been created, the created member is removed to
   * prevent orphaned records.
   *
   * @param request member creation data
   * @throws ConflictException if a user with the provided email already exists
   * @throws NotFoundException if the created member cannot be retrieved
   */
  public void addMember(AddMemberRequestDTO request) {
    if (emailExists(request.email())) {
      throw new ConflictException(
          String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.CREATE, 409)));
    }

    Member member = createMemberFromRequest(request);
    User user = createUserFromRequest(request);

    try {
      log.debug("Inserting new member into database");
      dbService.insert("members", member);
      log.debug("Member inserted successfully");

      Map<String, Object> query = Map.of("selector", Map.of("email", request.email()), "limit", 1);
      List<Member> members = dbService.findByQuery("members", query, Member.class);

      if (members.isEmpty()) {
        throw new NotFoundException(
            String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.CREATE, 404)));
      }

      log.debug("Member retrieved successfully after creation");

      String temporaryPassword = AuthService.generatePassword(3, 2);

      Member savedMember = members.getFirst();
      user.setMemberId(savedMember.getId());
      user.setPassword(passwordEncoder.encode(temporaryPassword));
      user.setUserActive(member.getStatus().equals("active"));

      log.debug("Inserting linked user into database");
      dbService.insert("users", user);
      log.debug("Linked user inserted successfully");

      if (user.getUserActive()) {
        log.debug("Sending new user email");
        mailService.sendMail(
            user.getEmail(),
            "GVW-Office: Temporäres Password",
            "newUser",
            Map.of("tempPassword", temporaryPassword));
        log.debug("New user email sent successfully");
      }

      try {
        sseService.broadcastRefresh("MEMBERS");
      } catch (RuntimeException ex) {
        log.warn("Failed to broadcast MEMBERS refresh", ex);
      }
    } catch (Exception e) {
      log.error("Failed to create member and linked user. Starting rollback", e);

      // Rollback
      try {
        Map<String, Object> rollbackQuery =
            Map.of("selector", Map.of("email", request.email()), "limit", 1);
        List<Member> toDeleteList = dbService.findByQuery("members", rollbackQuery, Member.class);

        if (!toDeleteList.isEmpty()) {
          Member orphan = toDeleteList.getFirst();

          dbService.delete("members", orphan.getId(), orphan.getRev());
          log.debug("Rollback: Successfully deleted orphan member {}", orphan.getId());
        }
      } catch (Exception rollbackException) {
        log.error(
            "CRITICAL: Manual intervention required. Could not delete orphan member for email: {}",
            request.email(),
            rollbackException);
      }

      throw new RuntimeException(
          String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.CREATE, 500)), e);
    }
  }

  /**
   * Deletes a member and its linked user account.
   *
   * <p>Removes both the member record and the associated authentication user record, then
   * broadcasts a member refresh event.
   *
   * @param id identifier of the member
   * @throws BadRequestException if the identifier is invalid
   * @throws NotFoundException if the member or linked user does not exist
   */
  public void deleteMember(String id) {
    if (id == null || id.isEmpty()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.DELETE, 400)));
    }

    Member member = getMemberById(id, ErrorAction.DELETE);
    User user = getUserByMemberId(id, ErrorAction.DELETE);

    log.debug("Deleting member from database");
    dbService.delete("members", member.getId(), member.getRev());
    log.debug("Member deleted successfully");

    log.debug("Deleting linked user from database");
    dbService.delete("users", user.getId(), user.getRev());
    log.debug("Linked user deleted successfully");

    try {
      sseService.broadcastRefresh("MEMBERS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast MEMBERS refresh", ex);
    }
  }

  /**
   * Updates member information and the associated user account.
   *
   * <p>Updates both records to keep member data and authentication data synchronized. After a
   * successful update, a member refresh event is broadcast.
   *
   * @param request updated member information
   * @return list containing the new revisions of the member and user records
   * @throws NotFoundException if the member or linked user does not exist
   */
  public List<String> updateMember(UpdateMemberRequestDTO request) {
    // Can throw not found
    Member member = getMemberById(request.id(), ErrorAction.UPDATE);
    User user = getUserByMemberId(request.id(), ErrorAction.UPDATE);

    boolean statusChanged = !Objects.equals(member.getStatus(), request.status());

    Member originalMember = member;

    memberMapper.updateMemberFromDto(request, member);
    memberMapper.updateUserFromDto(request, user);

    member.setRev(request.rev());

    String temporaryPassword = AuthService.generatePassword(3, 2);

    if (statusChanged && request.status().equals("active")) {
      user.setPassword(passwordEncoder.encode(temporaryPassword));
      user.setUserActive(true);
      user.setChangePassword(true);
      member.setStatus("active");
    } else {
      user.setUserActive(false);
      member.setStatus("inactive");
    }

    log.debug("Updating member in database");

    Map<String, Object> memberResult = dbService.update("members", member.getId(), member);

    Map<String, Object> userResult;

    try {
      log.debug("Updating linked user in database");
      userResult = dbService.update("users", user.getId(), user);
    } catch (RuntimeException ex) {
      log.error("Linked user update failed, attempting to roll back member update", ex);

      try {
        originalMember.setRev((String) memberResult.get("rev"));
        dbService.update("members", originalMember.getId(), originalMember);

        log.debug("Member update rolled back successfully");
      } catch (RuntimeException rollbackEx) {
        log.error("Failed to roll back member update", rollbackEx);
      }

      throw ex;
    }

    if (statusChanged && request.status().equals("active")) {
      log.debug("Sending new user email");

      mailService.sendMail(
          user.getEmail(),
          "GVW-Office: Temporäres Password",
          "resetPassword",
          Map.of("tempPassword", temporaryPassword));

      log.debug("New user email sent successfully");
    }

    try {
      sseService.broadcastRefresh("MEMBERS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast MEMBERS refresh", ex);
    }

    if (memberResult != null
        && memberResult.containsKey("rev")
        && userResult != null
        && userResult.containsKey("rev")) {
      return List.of((String) memberResult.get("rev"), (String) userResult.get("rev"));
    }

    log.error("Member update failed: database response did not contain valid revisions");
    throw new RuntimeException(
        String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.UPDATE, 500)));
  }

  /**
   * Toggles the active state of a member.
   *
   * <p>Switches the member status between {@code active} and {@code inactive} and broadcasts a
   * member refresh event after updating.
   *
   * @param id identifier of the member
   * @param _rev current database revision of the member
   * @return new database revision of the updated member
   * @throws BadRequestException if the identifier is invalid
   * @throws NotFoundException if the member does not exist
   */
  public List<String> updateMemberStatus(String id, String _rev) {
    if (id == null || id.isEmpty()) {
      throw new BadRequestException(
          String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.UPDATE, 400)));
    }

    Member member = getMemberById(id, ErrorAction.UPDATE);
    User user = getUserByMemberId(id, ErrorAction.UPDATE);

    member.setRev(_rev);
    member.setStatus("active".equals(member.getStatus()) ? "inactive" : "active");

    String temporaryPassword = AuthService.generatePassword(3, 2);
    if (member.getStatus().equals("active")) {
      user.setPassword(passwordEncoder.encode(temporaryPassword));
      user.setUserActive(true);
      user.setChangePassword(true);
    } else {
      user.setUserActive(false);
    }

    log.debug("Updating user active in database");
    Map<String, Object> userResult = dbService.update("users", user.getId(), user);
    log.debug("User active updated successfully");

    log.debug("Updating member status in database");
    Map<String, Object> memberResult = dbService.update("members", member.getId(), member);
    log.debug("Member status updated successfully");

    if (user.getUserActive()) {
      log.debug("Sending new user email");
      mailService.sendMail(
          user.getEmail(),
          "GVW-Office: Temporäres Password",
          "resetPassword",
          Map.of("tempPassword", temporaryPassword));
      log.debug("New user email sent successfully");
    }

    try {
      sseService.broadcastRefresh("MEMBERS");
    } catch (RuntimeException ex) {
      log.warn("Failed to broadcast MEMBERS refresh", ex);
    }

    if (memberResult != null && memberResult.containsKey("rev")) {
      return List.of((String) memberResult.get("rev"), (String) userResult.get("rev"));
    }

    log.error("Member status update failed: database response did not contain a revision");
    throw new RuntimeException(
        String.valueOf(ErrorDomain.MEMBER.createCode(ErrorAction.UPDATE, 500)));
  }

  /**
   * Checks whether an email address is already associated with a user account.
   *
   * @param email email address to check
   * @return {@code true} if a user with the email exists
   */
  private boolean emailExists(String email) {
    Map<String, Object> query = Map.of("selector", Map.of("email", email), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);

    return !users.isEmpty();
  }

  /**
   * Creates a user entity from a member creation request.
   *
   * <p>Initializes default authentication state including first login requirement, generated user
   * identifier, and assigned role.
   *
   * @param request member creation data
   * @return initialized user entity
   */
  private User createUserFromRequest(AddMemberRequestDTO request) {
    User user = new User();
    user.setEmail(request.email());
    user.setName(request.name() + " " + request.surname());
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

  /**
   * Creates a member entity from a creation request.
   *
   * @param request member creation data
   * @return initialized member entity
   */
  private Member createMemberFromRequest(AddMemberRequestDTO request) {
    Member member = new Member();
    member.setName(request.name());
    member.setSurname(request.surname());
    member.setEmail(request.email());
    member.setPhone(request.phone());
    member.setAddress(request.address());
    member.setVoice(request.voice());
    member.setRole(Role.fromString(request.role()));
    member.setStatus(request.status());
    member.setBirthdate(request.birthdate());
    member.setJoined(request.joined());

    return member;
  }

  /**
   * Retrieves a member by database identifier.
   *
   * @param id identifier of the member
   * @param action action context used for generating error codes
   * @return matching member entity
   * @throws NotFoundException if no member exists with the given identifier
   */
  private Member getMemberById(String id, ErrorAction action) {
    Member member = dbService.findById("members", id, Member.class);

    if (member == null)
      throw new NotFoundException(String.valueOf(ErrorDomain.MEMBER.createCode(action, 404)));

    return member;
  }

  /**
   * Retrieves the user account linked to a member.
   *
   * @param memberId identifier of the linked member
   * @param action action context used for generating error codes
   * @return linked user entity
   * @throws NotFoundException if no linked user exists
   */
  private User getUserByMemberId(String memberId, ErrorAction action) {
    Map<String, Object> query = Map.of("selector", Map.of("memberId", memberId), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);

    if (users == null || users.isEmpty())
      throw new NotFoundException(String.valueOf(ErrorDomain.MEMBER.createCode(action, 404)));

    return users.getFirst();
  }
}
