package com.williamsolari.portfolio.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String CONTACT_EMAIL = "contact@williamsolari.com";
    private static final String FROM_NAME = "William Solari Portfolio";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendContactMessage(String name, String replyToEmail, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(fromEmail, FROM_NAME);
            helper.setTo(CONTACT_EMAIL);
            helper.setReplyTo(replyToEmail);
            helper.setSubject("Portfolio contact from " + name);
            helper.setText("""
                    New message from the portfolio contact form.

                    Name: %s
                    Email: %s

                    %s
                    """.formatted(name, replyToEmail, message));
            mailSender.send(mimeMessage);
            log.info("Contact message sent from {}", replyToEmail);
        } catch (Exception e) {
            log.error("Failed to send contact message from {}: {}", replyToEmail, e.getMessage());
            throw new RuntimeException("Failed to send message. Please try again later.");
        }
    }
}
