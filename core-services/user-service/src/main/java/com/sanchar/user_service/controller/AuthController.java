package com.sanchar.user_service.controller;


import com.sanchar.common_library.dto.ApiResponse;
import com.sanchar.user_service.dto.RegisterRequest;
import com.sanchar.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Success", authService.register(req)));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verify(@RequestParam String email, @RequestParam String otp) {
        return ResponseEntity.ok(ApiResponse.success("Success", authService.verifyOtp(email, otp)));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<String>> resendOtp(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.success("Success", authService.resendOtp(email)));
    }
}
