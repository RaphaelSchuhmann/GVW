package com.gvw.gvwbackend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class HashUtil {
  public String createHash(String value) {
    if (value == null) return null;

    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = md.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(encodedHash);
    } catch (NoSuchAlgorithmException e) {
      // This should NEVER happen on a normal JVM
      throw new IllegalStateException("JVM does not support SHA-256", e);
    }
  }

  public boolean compare(String rawInput, String storedHash) {
    if (rawInput == null || storedHash == null) return false;

    String inputHash = createHash(rawInput);

    return MessageDigest.isEqual(
        inputHash.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
  }
}
