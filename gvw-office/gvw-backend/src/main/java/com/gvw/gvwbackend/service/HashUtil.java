package com.gvw.gvwbackend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Utility component for creating and comparing SHA-256 hashes.
 *
 * <p>Used for deterministic hashing of values where the original value should not be stored
 * directly. The generated hashes are represented as lowercase hexadecimal strings.
 */
@Component
public class HashUtil {
  /**
   * Creates a SHA-256 hash from a string value.
   *
   * <p>The input is encoded using UTF-8 before hashing. The resulting digest is returned as a
   * lowercase hexadecimal string.
   *
   * @param value value to hash
   * @return SHA-256 hash as hexadecimal string, or {@code null} if the input value is null
   * @throws IllegalStateException if the JVM does not provide SHA-256 support
   */
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

  /**
   * Compares a raw input value against a previously stored hash.
   *
   * <p>The input value is hashed using the same SHA-256 algorithm and compared against the stored
   * hash using a constant-time comparison to reduce the risk of timing attacks.
   *
   * @param rawInput unhashed input value
   * @param storedHash previously generated SHA-256 hash
   * @return {@code true} if the generated hash matches the stored hash, otherwise {@code false}
   */
  public boolean compare(String rawInput, String storedHash) {
    if (rawInput == null || storedHash == null) return false;

    String inputHash = createHash(rawInput);

    return MessageDigest.isEqual(
        inputHash.getBytes(StandardCharsets.UTF_8), storedHash.getBytes(StandardCharsets.UTF_8));
  }
}
