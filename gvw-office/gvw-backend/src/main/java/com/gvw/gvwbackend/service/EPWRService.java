package com.gvw.gvwbackend.service;

import com.gvw.gvwbackend.dto.response.NewEmergencyTokenDTO;
import com.gvw.gvwbackend.exception.InvalidCredentialsException;
import com.gvw.gvwbackend.exception.NotFoundException;
import com.gvw.gvwbackend.model.EPWRToken;
import com.gvw.gvwbackend.model.Role;
import com.gvw.gvwbackend.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

  public NewEmergencyTokenDTO getNewEmergencyToken() {
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

      if (!dbService.insert("emergency_token", epwrToken)) {
        throw new RuntimeException("Failed to insert emergency token");
      }
    } else {
      EPWRToken savedToken = tokenList.getFirst();
      savedToken.setHashedToken(hashedToken);
      savedToken.setCreatedAt(Instant.now());
      savedToken.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

      // Note that insert here is doing the update by replacing the already existing entry
      if (!dbService.insert("emergency_token", savedToken)) {
        throw new RuntimeException("Failed to update emergency token");
      }
    }

    log.info("Emergency token manually regenerated");
    return new NewEmergencyTokenDTO(token);
  }

  public NewEmergencyTokenDTO useEmergencyToken(String token) {
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
      log.warn("No admins found during emergency access!");
    }

    processAdminPasswordResets(admins);
    notifyAdminsOfUsage(admins);

    return new NewEmergencyTokenDTO(newToken);
  }

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
      log.warn("Emergency token invalid!");
      throw new InvalidCredentialsException("TokenInvalid");
    }

    return savedToken;
  }

  private void processAdminPasswordResets(List<User> admins) {
    for (User admin : admins) {
      String tempPw = AuthService.generatePassword(3, 2);
      admin.setPassword(passwordEncoder.encode(tempPw));
      admin.setChangePassword(true);

      if (!dbService.insert("users", admin)) {
        throw new RuntimeException("Failed to update admin password");
      }

      mailService.sendMail(
          admin.getEmail(),
          "GVW-Office: Passwort zurückgesetzt",
          "resetPassword",
          Map.of("tempPassword", tempPw));
    }
  }

  private boolean tryUpdateEmergencyToken(EPWRToken savedToken, String newToken) {
    savedToken.setHashedToken(hashUtil.createHash(newToken));
    savedToken.setCreatedAt(Instant.now());
    savedToken.setExpiresAt(Instant.now().plus(Duration.ofDays(30)));

    return dbService.insert("emergency_token", savedToken);
  }

  private void notifyAdminsOfUsage(List<User> admins) {
    admins.forEach(
        admin ->
            mailService.sendMail(
                admin.getEmail(), "Notfallzugang verwendet", "emergencyTokenUsed", Map.of()));
  }
}
