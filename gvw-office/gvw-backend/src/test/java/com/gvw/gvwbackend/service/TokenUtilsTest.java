package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TokenUtilsTest {
  @Test
  void testGenerateTokenShouldReturnNotNullString() {
    String token = TokenUtils.generateToken(32);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void testGenerateTokenShouldReturnDifferentTokensEachCall() {
    String token1 = TokenUtils.generateToken(32);
    String token2 = TokenUtils.generateToken(32);

    assertNotEquals(token1, token2);
  }

  @Test
  void testGenerateTokenShouldRespectByteLength() {
    int byteLength = 32;

    String token = TokenUtils.generateToken(byteLength);

    int expectedLength = (int) Math.ceil(byteLength / 3.0) * 4;

    assertTrue(token.length() <= expectedLength);
    assertTrue(token.length() >= expectedLength - 2);
  }

  @Test
  void testGenerateTokenShouldWorkWithZeroLength() {
    String token = TokenUtils.generateToken(0);

    assertNotNull(token);
    assertEquals("", token);
  }

  @Test
  void testGenerateTokenShouldDefaultUse64Bytes() {
    String token = TokenUtils.generateToken();

    int expectedLength = (int) Math.ceil(64 / 3.0) * 4;

    assertTrue(token.length() <= expectedLength);
    assertTrue(token.length() >= expectedLength - 2);
  }

  @Test
  void testGenerateTokenShouldOnlyContainBase64Characters() {
    String token = TokenUtils.generateToken(32);

    assertTrue(token.matches("^[A-Za-z0-9+/]*$"));
  }
}
