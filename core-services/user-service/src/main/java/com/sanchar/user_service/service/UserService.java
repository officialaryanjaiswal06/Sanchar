package com.sanchar.user_service.service;


import com.sanchar.user_service.dto.UserProfileResponse;
import com.sanchar.user_service.model.User;
import com.sanchar.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public void updateProfilePicture(String userId, String imageUrl) {
        // 1. Fetch the user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // 2. Set the new image URL
        user.setProfilePictureUrl(imageUrl);

        // 3. Save back to MongoDB
        userRepository.save(user);
    }

    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserProfileResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender().name())
                .bio(user.getBio())
                .profilePictureUrl(user.getProfilePictureUrl())
                .joinedAt(user.getCreatedAt())
                .build();
    }
}
