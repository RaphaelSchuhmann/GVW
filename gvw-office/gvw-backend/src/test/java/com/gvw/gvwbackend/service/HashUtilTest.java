package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HashUtilTest {
  private HashUtil hashUtil;

  @BeforeEach()
  void setUp() {
    hashUtil = new HashUtil();
  }

  @Test
  void testCreateHashShouldReturnConsistentHashForSameInput() {
    String input = "test123";

    String hash1 = hashUtil.createHash(input);
    String hash2 = hashUtil.createHash(input);

    assertNotNull(hash1);
    assertEquals(hash1, hash2);
  }

  @Test
  void testCreateHashShouldReturnDifferentHashesForDifferentInput() {
    String hash1 = hashUtil.createHash("test123");
    String hash2 = hashUtil.createHash("test3214");

    assertNotEquals(hash1, hash2);
  }

  @Test
  void testCreateHashShouldReturnNullWhenInputIsNull() {
    String hash = hashUtil.createHash(null);

    assertNull(hash);
  }

  @Test
  void testCompareShouldReturnTrueForMatchingInput() {
    String raw = "mySecret";
    String hash = hashUtil.createHash(raw);

    boolean result = hashUtil.compare(raw, hash);

    assertTrue(result);
  }

  @Test
  void testCompareShouldReturnFalseForNonMatchingInput() {
    String raw = "mySecret";
    String hash = hashUtil.createHash(raw);

    boolean result = hashUtil.compare("wrongInput", hash);

    assertFalse(result);
  }

  @Test
  void testCompareShouldReturnFalseWhenRawInputIsNull() {
    String hash = hashUtil.createHash("test");

    boolean result = hashUtil.compare(null, hash);

    assertFalse(result);
  }

  @Test
  void testCompareShouldReturnFalseWhenStoredHashIsNull() {
    boolean result = hashUtil.compare("test", null);

    assertFalse(result);
  }

  @Test
  void testCompareShouldReturnFalseWhenBothAreNull() {
    boolean result = hashUtil.compare(null, null);

    assertFalse(result);
  }
}
