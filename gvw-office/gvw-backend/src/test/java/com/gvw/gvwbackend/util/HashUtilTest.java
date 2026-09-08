package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HashUtilTest {

  @InjectMocks private HashUtil hashUtil;

  @Test
  void createHash_Success() {
    String result = hashUtil.createHash("test-value");

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(64, result.length()); // SHA-256 produces 64 hex characters
  }

  @Test
  void createHash_SameInput_SameOutput() {
    String hash1 = hashUtil.createHash("test-value");
    String hash2 = hashUtil.createHash("test-value");

    assertEquals(hash1, hash2);
  }

  @Test
  void createHash_DifferentInput_DifferentOutput() {
    String hash1 = hashUtil.createHash("test-value-1");
    String hash2 = hashUtil.createHash("test-value-2");

    assertNotEquals(hash1, hash2);
  }

  @Test
  void createHash_EmptyString_ReturnsHash() {
    String result = hashUtil.createHash("");

    assertNotNull(result);
    assertFalse(result.isEmpty());
  }

  @Test
  void createHash_NullInput_ReturnsNull() {
    String result = hashUtil.createHash(null);

    assertNull(result);
  }

  @Test
  void compare_MatchingHashes_ReturnsTrue() {
    String hash = hashUtil.createHash("test-value");

    boolean result = hashUtil.compare("test-value", hash);

    assertTrue(result);
  }

  @Test
  void compare_NonMatchingHashes_ReturnsFalse() {
    String hash = hashUtil.createHash("test-value");

    boolean result = hashUtil.compare("wrong-value", hash);

    assertFalse(result);
  }

  @Test
  void compare_NullValue_ReturnsFalse() {
    String hash = hashUtil.createHash("test-value");

    boolean result = hashUtil.compare(null, hash);

    assertFalse(result);
  }

  @Test
  void compare_NullHash_ReturnsFalse() {
    boolean result = hashUtil.compare("test-value", null);

    assertFalse(result);
  }

  @Test
  void compare_BothNull_ReturnsFalse() {
    boolean result = hashUtil.compare(null, null);

    assertFalse(result);
  }
}
