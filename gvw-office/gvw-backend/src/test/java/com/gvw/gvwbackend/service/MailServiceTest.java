package com.gvw.gvwbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
class MailServiceTest {

  @Mock private JavaMailSender mailSender;

  @Mock private TemplateEngine templateEngine;

  @InjectMocks private MailService mailService;

  private static final String FROM_EMAIL = "noreply@example.com";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(mailService, "fromEmail", FROM_EMAIL);
  }

  @Test
  void sendMail_Success() {
    MimeMessage mimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test</html>");

    assertDoesNotThrow(
        () -> mailService.sendMail("test@example.com", "Test Subject", "test-template", Map.of()));

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendMail_TemplateEngineFailure_ThrowsRuntimeException() {
    when(templateEngine.process(anyString(), any(Context.class)))
        .thenThrow(new RuntimeException("Template error"));

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () ->
                mailService.sendMail(
                    "test@example.com", "Test Subject", "test-template", Map.of()));
    assertEquals("Error sending mail", ex.getMessage());
    assertEquals("Template error", ex.getCause().getMessage());
  }

  @Test
  void sendMail_SendFailure_ThrowsRuntimeException() {
    MimeMessage mimeMessage = mock(MimeMessage.class);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test</html>");
    doThrow(new RuntimeException("Send error")).when(mailSender).send(any(MimeMessage.class));

    assertThrows(
        RuntimeException.class,
        () -> mailService.sendMail("test@example.com", "Test Subject", "test-template", Map.of()));
  }
}
