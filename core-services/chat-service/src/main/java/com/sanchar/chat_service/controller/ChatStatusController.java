//package com.sanchar.chat_service.controller;
//
//import com.sanchar.common_library.dto.ApiResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequestMapping("/chat/status")
//@RequiredArgsConstructor
//public class ChatStatusController {
//
//    private final RedisTemplate<String, Object> redisTemplate;
//
//    @GetMapping("/{userId}")
//    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getUserStatus(@PathVariable String userId) {
//        // 1. Check Redis Key (Matches the key set in WebSocketEventListener)
//        String key = "status:online:" + userId;
//        Boolean isOnline = redisTemplate.hasKey(key);
//
//        return ResponseEntity.ok(ApiResponse.success(
//                "Status Fetched",
//                Map.of("online", Boolean.TRUE.equals(isOnline))
//        ));
//    }
//}

package com.sanchar.chat_service.controller;

import com.sanchar.common_library.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate; // IMPORT THIS
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/chat/status")
@RequiredArgsConstructor
public class ChatStatusController {

    // MATCHING TEMPLATE
    private final StringRedisTemplate redisTemplate;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getUserStatus(@PathVariable String userId) {

        String key = "status:online:" + userId;

        // This will now find the key because serialization is String-only
        Boolean isOnline = redisTemplate.hasKey(key);

        return ResponseEntity.ok(ApiResponse.success(
                "Status Fetched",
                Map.of("online", Boolean.TRUE.equals(isOnline))
        ));
    }
}