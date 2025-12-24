package com.sanchar.notification_service.service;

import com.sanchar.common_library.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final JavaMailSender mailSender;

    // Hardcode the queue name or move to config.
    // MUST MATCH 'user-service' configuration EXACTLY.
    @Value("${spring.mail.username}")
    private String senderEmail;
    private static final String QUEUE_NAME = "user.registered.queue";


    @RabbitListener(queues = QUEUE_NAME)
    public void handleUserRegistration(UserRegisteredEvent event) {
        log.info("📩 New Event Received for: {}", event.getEmail());

        // Validate data just in case
        if (event.getEmail() == null || event.getOtpCode() == null) {
            log.warn("⚠️ Invalid Event Data, skipping email.");
            return;
        }

        sendEmail(event.getEmail(), event.getUsername(), event.getOtpCode());
    }

    private void sendEmail(String to, String username, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail); // Sender must match config
            message.setTo(to);
            message.setSubject("Welcome to Sanchar! Verify your Account");
            message.setText("Hello " + username + ",\n\n" +
                    "Welcome to Sanchar. Please verify your account.\n" +
                    "Your OTP Code is: " + otp + "\n\n" +
                    "This code expires in 10 minutes.\n\n" +
                    "Best Regards,\nSanchar Team");

            mailSender.send(message);
            log.info("✅ Email successfully sent to {}", to);
        } catch (Exception e) {
            log.error("❌ Failed to send email", e);
        }
    }
}
