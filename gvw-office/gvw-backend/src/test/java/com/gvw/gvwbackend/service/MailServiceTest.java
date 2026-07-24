package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
public class MailServiceTest {
  @Mock private JavaMailSender mailSender;
  @Mock private TemplateEngine templateEngine;
  @InjectMocks private MailService mailService;

  private final String FROM_EMAIL = "info@gvw-choir.de";

  @BeforeEach
  void setup() {
    ReflectionTestUtils.setField(mailService, "fromEmail", FROM_EMAIL);
  }

  @Test
  void testSendMailSuccess() throws Exception {
    String to = "member@test.com";
    String subject = "Welcome!";
    String templateName = "newUser";
    Map<String, Object> variables = Map.of("tempPassword", "secret123");
    String mockHtml = "<html><body>Welcome!</body></html>";

    MimeMessage mimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

    when(templateEngine.process(eq(templateName), any(Context.class))).thenReturn(mockHtml);

    assertDoesNotThrow(() -> mailService.sendMail(to, subject, templateName, variables));

    verify(templateEngine).process(eq(templateName), any(Context.class));
    verify(mailSender).send(any(MimeMessage.class));
  }
}
