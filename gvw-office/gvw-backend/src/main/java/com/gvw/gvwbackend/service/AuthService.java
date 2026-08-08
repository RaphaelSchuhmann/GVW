package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.request.ChangePwRequestDTO;
import com.gvw.gvwbackend.dto.request.LoginRequestDTO;
import com.gvw.gvwbackend.dto.response.AutoLoginResponseDTO;
import com.gvw.gvwbackend.dto.response.LoginResponseDTO;
import com.gvw.gvwbackend.exception.*;
import com.gvw.gvwbackend.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles authentication-related operations.
 *
 * <p>This service is responsible for:
 *
 * <ul>
 *   <li>User login and JWT generation
 *   <li>Failed login attempt tracking and temporary account locking
 *   <li>Password changes
 *   <li>Automatic login validation for existing sessions
 *   <li>Generation of temporary passwords for recovery flows
 * </ul>
 *
 * <p>User data is stored in CouchDB through {@link DbService}.
 */
@Service
public class AuthService {

  private final DbService dbService;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private static final List<String> words =
      List.of(
          "apple",
          "banana",
          "chorus",
          "melody",
          "note",
          "voice",
          "sing",
          "harmony",
          "music",
          "choir",
          "pineapple",
          "gvw");

  public AuthService(DbService dbService, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.dbService = dbService;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  /**
   * Authenticates a user and creates a JWT token.
   *
   * <p>The login flow:
   *
   * <ol>
   *   <li>Loads the user by email.
   *   <li>Checks whether the account is currently locked.
   *   <li>Validates the supplied password.
   *   <li>Tracks failed login attempts and locks the account after repeated failures.
   *   <li>Resets failed attempts after a successful login.
   *   <li>Creates a JWT containing the user's ID and role.
   * </ol>
   *
   * <p>Failed login attempts are stored persistently so that restarting the backend does not reset
   * the protection mechanism.
   *
   * @param requestDTO login credentials
   * @return JWT token and additional login state information
   * @throws InvalidCredentialsException if credentials are invalid
   * @throws TooManyRequestsException if the account is temporarily locked
   */
  public LoginResponseDTO login(LoginRequestDTO requestDTO) {
    Map<String, Object> query = Map.of("selector", Map.of("email", requestDTO.email()), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);

    if (users.isEmpty())
      throw new InvalidCredentialsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401)));

    User user = users.getFirst();
    Instant now = Instant.now();

    if (user.getLockUntil() != null && now.isBefore(user.getLockUntil())) {
      throw new TooManyRequestsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 429)),
          user.getLockUntil().toEpochMilli());
    }

    String rev;

    if (!passwordEncoder.matches(requestDTO.password(), user.getPassword())) {
      int failedAttempts = Optional.ofNullable(user.getFailedLoginAttempts()).orElse(0);
      if (failedAttempts >= 4) {
        user.setLockUntil(Instant.now().plus(Duration.ofMinutes(15)));
        dbService.update("users", user.getId(), user);

        throw new TooManyRequestsException(
            String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 429)),
            user.getLockUntil().toEpochMilli());
      } else {
        user.setFailedLoginAttempts(failedAttempts + 1);
        dbService.update("users", user.getId(), user);

        throw new InvalidCredentialsException(
            String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401)));
      }
    } else {
      user.setFailedLoginAttempts(0);
      user.setLockUntil(null);
      Map<String, Object> resp = dbService.update("users", user.getId(), user);

      if (resp != null && resp.containsKey("rev")) {
        rev = (String) resp.get("rev");
      } else {
        throw new RuntimeException(
            String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 500)));
      }
    }

    String token =
        jwtService.generateToken(user.getUserId(), Map.of("role", user.getRole().getValue()));

    return new LoginResponseDTO(
        token,
        Boolean.TRUE.equals(user.getChangePassword()),
        Boolean.TRUE.equals(user.getFirstLogin()),
        rev);
  }

  /**
   * Changes the password of an existing user.
   *
   * <p>The old password must be verified before changing the password. New passwords cannot be
   * identical to the current password.
   *
   * <p>This method also clears account flags related to first login and forced password changes.
   *
   * @param requestDTO password change request containing user credentials
   * @return updated CouchDB document revision
   */
  public String changePassword(ChangePwRequestDTO requestDTO) {
    Map<String, Object> query = Map.of("selector", Map.of("email", requestDTO.email()), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);

    if (users.isEmpty())
      throw new InvalidCredentialsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.UPDATE, 401)));

    User user = users.getFirst();

    // Ensure new password is not the same as old password
    if (passwordEncoder.matches(requestDTO.newPassword(), user.getPassword())) {
      throw new ConflictException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.UPDATE, 409)));
    }

    // Authenticate user
    if (!passwordEncoder.matches(requestDTO.oldPassword(), user.getPassword())) {
      throw new InvalidCredentialsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.UPDATE, 401)));
    }

    String hashedPassword = passwordEncoder.encode(requestDTO.newPassword());
    user.setPassword(hashedPassword);
    user.setChangePassword(false);
    user.setFirstLogin(false);

    Map<String, Object> resp = dbService.update("users", user.getId(), user);

    if (resp != null && resp.containsKey("rev")) {
      return (String) resp.get("rev");
    }

    throw new RuntimeException(
        String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.UPDATE, 500)));
  }

  /**
   * Retrieves user information required for automatic login.
   *
   * <p>This does not authenticate a user or create a token. It only validates that the user still
   * exists and returns account state information required by the frontend.
   *
   * @param id internal user identifier
   * @return user information required for automatic login handling
   */
  public AutoLoginResponseDTO autoLogin(String id) {
    if (id == null || id.isBlank()) {
      throw new InvalidCredentialsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401)));
    }

    Map<String, Object> query = Map.of("selector", Map.of("userId", id), "limit", 1);
    List<User> users = dbService.findByQuery("users", query, User.class);
    if (users == null || users.isEmpty()) {
      throw new InvalidCredentialsException(
          String.valueOf(ErrorDomain.AUTH.createCode(ErrorAction.AUTH, 401)));
    }

    User user = users.getFirst();

    return new AutoLoginResponseDTO(
        user.getEmail(),
        Boolean.TRUE.equals(user.getChangePassword()),
        Boolean.TRUE.equals(user.getFirstLogin()));
  }

  /**
   * Generates a temporary human-readable password.
   *
   * <p>The generated password consists of randomly selected words combined with random digits.
   * Words are capitalized and all generated parts are shuffled before being combined.
   *
   * <p>This method uses {@link java.security.SecureRandom} to avoid predictable password
   * generation.
   *
   * @param wordCount number of random words to include
   * @param numberCount number of random digits to include
   * @return generated temporary password
   */
  public static String generatePassword(int wordCount, int numberCount) {
    java.security.SecureRandom random = new java.security.SecureRandom();

    List<String> chosenWords = new ArrayList<>();
    for (int i = 0; i < wordCount; i++) {
      String word = words.get(random.nextInt(words.size()));
      String capitalizedWord = word.substring(0, 1).toUpperCase() + word.substring(1);
      chosenWords.add(capitalizedWord);
    }

    List<String> digits = new ArrayList<>();
    for (int i = 0; i < numberCount; i++) {
      digits.add(Integer.toString(random.nextInt(10)));
    }

    List<String> combined = new ArrayList<>();
    combined.addAll(chosenWords);
    combined.addAll(digits);

    Collections.shuffle(combined, random);

    StringBuilder result = new StringBuilder();
    for (String s : combined) {
      result.append(s);
    }

    return result.toString();
  }
}
