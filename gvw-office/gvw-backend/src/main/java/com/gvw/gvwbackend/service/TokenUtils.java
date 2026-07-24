package com.gvw.gvwbackend.service;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtils {
  private static final SecureRandom secureRandom = new SecureRandom();

  public static String generateToken(int byteLength) {
    if (byteLength < 0) {
      throw new IllegalArgumentException("Byte length cannot be negative");
    }

    byte[] bytes = new byte[byteLength];
    secureRandom.nextBytes(bytes);
    return Base64.getEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String generateToken() {
    return generateToken(64);
  }
}
