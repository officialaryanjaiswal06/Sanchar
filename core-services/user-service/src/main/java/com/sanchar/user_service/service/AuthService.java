package com.sanchar.user_service.service;

import com.sanchar.common_library.event.UserRegisteredEvent;
import com.sanchar.user_service.config.RabbitMQConfig;
import com.sanchar.user_service.dto.RegisterRequest;
import com.sanchar.user_service.model.Gender;
import com.sanchar.user_service.model.OtpType;
import com.sanchar.user_service.model.User;
import com.sanchar.user_service.model.UserAuth;
import com.sanchar.user_service.repository.UserAuthRepository;
import com.sanchar.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor @Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final PasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public String register(RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) throw new RuntimeException("Username taken");
        if (userRepository.existsByEmail(req.getEmail())) throw new RuntimeException("Email taken");

        User user = userRepository.save(User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .fullName(req.getFullName())
                .gender(req.getGender() != null ? req.getGender() : Gender.OTHER)
                .bio(req.getBio() != null && !req.getBio().isEmpty() ? req.getBio() : "Hey! I am using Sanchar.")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        String otp = String.valueOf((int)(Math.random()*900000)+100000);

        userAuthRepository.save(UserAuth.builder()
                .userId(user.getId())
                .password(passwordEncoder.encode(req.getPassword()))
                .isAccountEnabled(false) // Locked!
                .otp(otp)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .otpType(OtpType.REGISTRATION)
                .build());

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY,
                new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFullName(), user.getUsername(), otp));

        return "User Registered. Verify OTP.";
    }



    public String verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserAuth auth = userAuthRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Auth record missing"));

        // 1. Check Block Status
        if (auth.isLocked()) {
            throw new RuntimeException("Account is LOCKED due to too many failed attempts. Contact Support.");
        }

        if (auth.isAccountEnabled()) {
            return "Account is already verified.";
        }

        // 2. Check Expiry
        if (auth.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        // 3. Check OTP Match
        if (!auth.getOtp().equals(otp)) {
            // INCREMENT FAILURE COUNT
            int newCount = auth.getAttemptCount() + 1;
            auth.setAttemptCount(newCount);

            // LOCK IF > 3
            if (newCount >= 3) {
                auth.setLocked(true);
                userAuthRepository.save(auth);
                throw new RuntimeException("Max attempts reached. Account is now LOCKED.");
            }

            userAuthRepository.save(auth);
            throw new RuntimeException("Invalid OTP. Attempts left: " + (3 - newCount));
        }

        // 4. Success Case
        auth.setAccountEnabled(true);
        auth.setOtp(null);
        auth.setAttemptCount(0); // Reset on success
        userAuthRepository.save(auth);

        return "Account verified successfully. You can login now.";
    }


    @Transactional
    public String resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserAuth auth = userAuthRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Auth record missing"));

        if (auth.isLocked()) {
            throw new RuntimeException("Account is LOCKED. Cannot resend OTP.");
        }
        if (auth.isAccountEnabled()) {
            return "Account is already verified.";
        }

        // 1. Generate New OTP
        String newOtp = String.valueOf((int)(Math.random() * 900000) + 100000);

        // 2. Update Auth (Reset attempt count to give them a fresh chance?)
        // Security decision: Usually we reset count when a NEW Code is generated.
        auth.setOtp(newOtp);
        auth.setOtpExpiry(LocalDateTime.now().plusMinutes(10)); // +10 Mins
        auth.setAttemptCount(0); // Reset failures for the new code

        userAuthRepository.save(auth);

        // 3. Trigger Email Event Again
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY,
                new UserRegisteredEvent(user.getId(), user.getEmail(), user.getFullName(), user.getUsername(), newOtp));

        return "New OTP sent to your email.";
    }

    public void updateUserProfilePicture(String userId, String imageUrl) {
        // 1. Find User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Update Field
        user.setProfilePictureUrl(imageUrl);

        // 3. Save to MongoDB
        userRepository.save(user);
    }

}
