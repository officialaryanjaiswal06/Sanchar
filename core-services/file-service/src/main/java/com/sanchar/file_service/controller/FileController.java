package com.sanchar.file_service.controller;
import com.sanchar.common_library.dto.ApiResponse;
import com.sanchar.file_service.client.UserClient;
import com.sanchar.file_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {
    private final FileStorageService fileStorageService;
    private  final UserClient userClient;

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId
    ) {
        // Basic Validation
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File cannot be empty", HttpStatus.BAD_REQUEST, "No file content received"));
        }


        try {
            // 1. Upload File to Storage
            String fileUrl = fileStorageService.uploadProfilePicture(file, userId);

            // 2. Call User Service (Sync)
            if (userId != null && !userId.isEmpty()) {
                log.info("Updating User Service for ID: {}", userId);
                userClient.updateProfilePic(userId, Map.of("url", fileUrl));
            }

            // 3. Return Success
            return ResponseEntity.ok(
                    ApiResponse.success("Upload successful and User updated", Map.of("url", fileUrl))
            );

        } catch (Exception e) {
            log.error("Process failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Process failed", HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
        }


    }
}
