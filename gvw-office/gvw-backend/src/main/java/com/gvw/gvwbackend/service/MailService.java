package com.gvw.gvwbackend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class MailService {
  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;
  private static final Logger log = LoggerFactory.getLogger(MailService.class);

  @Value("${spring.mail.username}")
  private String fromEmail;

  public MailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
  }

  public void sendMail(
      String to, String subject, String templateName, Map<String, Object> variables) {
    try {
      Context context = new Context();
      context.setVariables(variables);

      String htmlContent = templateEngine.process(templateName, context);

      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);

      ClassPathResource res = new ClassPathResource("static/images/logo.png");
      helper.addInline("logo-image", res);

      mailSender.send(mimeMessage);
    } catch (MessagingException e) {
      log.error("Error sending mail: {}", e.getMessage(), e);
      throw new RuntimeException("Error sending mail", e);
    }
  }
}
