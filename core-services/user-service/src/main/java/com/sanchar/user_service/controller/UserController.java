package com.sanchar.user_service.controller;


import com.sanchar.common_library.dto.ApiResponse;
import com.sanchar.user_service.dto.UserProfileResponse;
import com.sanchar.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users") // Separate base path from "/auth"
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;


    @PutMapping("/{userId}/profile-picture")
    public ResponseEntity<ApiResponse<Void>> updateProfilePicture(
            @PathVariable("userId") String userId,
            @RequestBody Map<String, String> requestBody
    ) {
        String imageUrl = requestBody.get("url");

        // Call the service layer to save to MongoDB
        userService.updateProfilePicture(userId, imageUrl);

        return ResponseEntity.ok(ApiResponse.success("Profile updated", null));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable String userId) {
        return ResponseEntity.ok(
                ApiResponse.success("Profile Fetched", userService.getUserProfile(userId))
        );
    }
}
