package com.gvw.gvwbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    properties = {
      "ALLOWED_ORIGINS=http://localhost:5173",
      "GVW_MAIL_USERNAME=test@example.com",
      "GVW_MAIL_PASSWORD=123test",
      "GVW_PORT=3500",
      "GVW_DB_USERNAME=test",
      "GVW_DB_PASSWORD=test",
      "GVW_JWT_SECRET=123jwt"
    })
class GvwBackendApplicationTests {

  @Test
  void contextLoads() {}
}
