package com.sanchar.user_service.controller;


import com.sanchar.common_library.dto.ApiResponse;
import com.sanchar.user_service.dto.UserProfileResponse;
import com.sanchar.user_service.dto.UserSearchDTO;
import com.sanchar.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSearchDTO>>> searchUsers(
            @RequestParam("query") String query,
            @RequestHeader("X-Authenticated-User") String currentUserId
    ) {
        if (query == null || query.trim().length() < 3) {
            // Optional: prevent searching for 1 letter (too many results)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Query must be at least 3 chars", HttpStatus.BAD_REQUEST, null));
        }

        List<UserSearchDTO> results = userService.searchUsers(currentUserId, query);
        return ResponseEntity.ok(ApiResponse.success("Users found", results));
    }
}
