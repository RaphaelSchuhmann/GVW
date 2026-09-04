package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.exception.InvalidCredentialsException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.EPWRToken;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import com.gvw.gvwbackend.util.HashUtil;
import com.gvw.gvwbackend.util.TokenUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

// =================================================
// SECURITY: Never store plaintext emergency tokens.
// Only return them at creation time.
// =================================================

/**
 * Service responsible for managing the emergency password reset (EPWR) system.
 *
 * <p>The emergency token provides a recovery mechanism for administrator access. Tokens are never
 * stored in plaintext. Only a SHA-256 hash is stored in the database, while the plaintext token is
 * returned only when generated.
 *
 * <p>Using an emergency token invalidates the previous token by generating a replacement token and
 * resets all administrator passwords.
 */
@Service
public class EPWRService {
  private final DbService dbService;
  private final PasswordEncoder passwordEncoder;
  private final MailService mailService;
  private final HashUtil hashUtil;
  private static final Logger log = LoggerFactory.getLogger(EPWRService.class);

  public EPWRService(
      DbService dbService,
      PasswordEncoder passwordEncoder,
      MailService mailService,
      HashUtil hashUtil) {
    this.dbService = dbService;
    this.passwordEncoder = passwordEncoder;
    this.mailService = mailService;
    this.hashUtil = hashUtil;
  }

  /**
   * Generates and stores a new emergency access token.
   *
   * <p>The generated token is returned once to the caller. Only its hash is persisted in the
   * database. If an emergency token already exists, it is replaced instead of creating an
   * additional token.
   *
   * <p>The generated token remains valid for 30 days.
   *
   * @return newly generated plaintext emergency token
   */
  public String getNewEmergencyToken() {
    String token = TokenUtils.generateToken();
    String hashedToken = hashUtil.createHash(token);

    List<EPWRToken> tokenList =
        dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class);

    if (tokenList.isEmpty()) {
      EPWRToken epwrToken = new EPWRToken();
      epwrToken.setHashedToken(hashedToken);
      epwrToken.setCreatedAt(Instant.now());
      epwrToken.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

      try {
        dbService.insert("emergency_token", epwrToken);
      } catch (Exception e) {
        log.error("EPWR [new]: unable to insert emergency token into database");
        throw new RuntimeException("Failed to insert emergency token");
      }
    } else {
      EPWRToken savedToken = tokenList.getFirst();
      savedToken.setHashedToken(hashedToken);
      savedToken.setCreatedAt(Instant.now());
      savedToken.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

      // DbService.insert() replaces the existing CouchDB document when an ID is present.
      // This is intentionally used instead of a separate update operation.
      try {
        dbService.insert("emergency_token", savedToken);
      } catch (Exception e) {
        log.error("EPWR [new]: unable to replace emergency token in database");
        throw new RuntimeException("Failed to update emergency token");
      }
    }

    log.info("Emergency token manually regenerated");
    return token;
  }

  /**
   * Uses an emergency token to restore administrator access.
   *
   * <p>The provided token is validated and immediately replaced with a new one to prevent reuse.
   * All administrator passwords are reset and temporary passwords are sent via email.
   *
   * <p>If multiple requests attempt to use the same token concurrently, only the first successful
   * update is accepted.
   *
   * @param token plaintext emergency token provided by the user
   * @return newly generated replacement emergency token
   * @throws InvalidCredentialsException if the token is invalid, expired, or already used
   * @throws NotFoundException if no emergency token exists
   */
  public String useEmergencyToken(String token) {
    EPWRToken savedToken = fetchAndValidateEmergencyToken(token);

    String newToken = TokenUtils.generateToken();
    boolean updated = tryUpdateEmergencyToken(savedToken, newToken);

    if (!updated) {
      log.warn("Emergency token already used concurrently");
      throw new InvalidCredentialsException("TokenAlreadyUsed");
    }

    List<User> admins =
        dbService.findByQuery("users", Map.of("selector", Map.of("role", Role.ADMIN)), User.class);

    if (admins.isEmpty()) {
      log.error("No admins found during emergency access!");
    }

    processAdminPasswordResets(admins);
    notifyAdminsOfUsage(admins);

    return newToken;
  }

  /**
   * Loads and validates the currently active emergency token.
   *
   * <p>Validation checks:
   *
   * <ul>
   *   <li>token existence
   *   <li>expiration date
   *   <li>hash equality against the stored token hash
   * </ul>
   *
   * @param token plaintext token provided by the user
   * @return validated token document
   */
  private EPWRToken fetchAndValidateEmergencyToken(String token) {
    List<EPWRToken> tokens =
        dbService.findByQuery(
            "emergency_token", Map.of("selector", Map.of(), "limit", 1), EPWRToken.class);

    if (tokens.isEmpty()) throw new NotFoundException("TokenNotFound");

    EPWRToken savedToken = tokens.getFirst();

    if (savedToken.getExpiresAt() != null && !savedToken.getExpiresAt().isAfter(Instant.now())) {
      throw new InvalidCredentialsException("TokenExpired");
    }

    if (!hashUtil.compare(token, savedToken.getHashedToken())) {
      log.warn("Emergency token invalid");
      throw new InvalidCredentialsException("TokenInvalid");
    }

    return savedToken;
  }

  /**
   * Resets passwords for all administrator accounts.
   *
   * <p>Each administrator receives a newly generated temporary password and is forced to change it
   * on the next login.
   *
   * @param admins administrators whose passwords should be reset
   */
  private void processAdminPasswordResets(List<User> admins) {
    for (User admin : admins) {
      String tempPw = AuthService.generatePassword(3, 2);
      admin.setPassword(passwordEncoder.encode(tempPw));
      admin.setChangePassword(true);

      try {
        dbService.insert("users", admin);
      } catch (Exception e) {
        log.error("Failed to update admin password");
        throw new RuntimeException("Failed to update admin password");
      }

      mailService.sendMail(
          admin.getEmail(),
          "GVW-Office: Passwort zurückgesetzt",
          "resetPassword",
          Map.of("tempPassword", tempPw));
    }
  }

  /**
   * Replaces the current emergency token with a newly generated one.
   *
   * <p>This operation also refreshes the creation and expiration timestamps. Returning false
   * indicates that the token could not be updated, which is treated as a possible concurrent token
   * usage attempt.
   *
   * @param savedToken currently stored token document
   * @param newToken replacement plaintext token
   * @return true if the replacement succeeded
   */
  private boolean tryUpdateEmergencyToken(EPWRToken savedToken, String newToken) {
    savedToken.setHashedToken(hashUtil.createHash(newToken));
    savedToken.setCreatedAt(Instant.now());
    savedToken.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

    // Note that if replacement / insertion fails it throws
    dbService.insert("emergency_token", savedToken);

    return true;
  }

  /**
   * Sends a notification email to administrators informing them that the emergency access mechanism
   * was used.
   *
   * @param admins administrators to notify
   */
  private void notifyAdminsOfUsage(List<User> admins) {
    admins.forEach(
        admin ->
            mailService.sendMail(
                admin.getEmail(), "Notfallzugang verwendet", "emergencyTokenUsed", Map.of()));
  }
}
