package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenUtilsTest {

  @Test
  void generateToken_DefaultLength_Success() {
    String token = TokenUtils.generateToken();

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateToken_CustomLength_Success() {
    String token = TokenUtils.generateToken(32);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateToken_DifferentCalls_DifferentTokens() {
    String token1 = TokenUtils.generateToken();
    String token2 = TokenUtils.generateToken();

    assertNotEquals(token1, token2);
  }

  @Test
  void generateToken_ZeroLength_ReturnsEmpty() {
    String token = TokenUtils.generateToken(0);

    assertNotNull(token);
    assertTrue(token.isEmpty());
  }

  @Test
  void generateToken_NegativeLength_ThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> TokenUtils.generateToken(-1));
  }

  @Test
  void generateToken_LargeLength_Success() {
    String token = TokenUtils.generateToken(128);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }
}
