package com.sanchar.user_service.controller;
import com.sanchar.common_library.dto.ApiResponse;
import com.sanchar.user_service.dto.FriendDTO;
import com.sanchar.user_service.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    // Public API: Request
    @PostMapping("/request/{targetId}")
    public ResponseEntity<ApiResponse<String>> sendRequest(
            @PathVariable String targetId,
            @RequestHeader("X-Authenticated-User") String requesterId // From Gateway
    ) {
        friendService.sendRequest(requesterId, targetId);
        return ResponseEntity.ok(ApiResponse.success("Request Sent", null));
    }

    // Public API: Accept
    @PutMapping("/accept/{requestId}")
    public ResponseEntity<ApiResponse<String>> acceptRequest(
            @PathVariable String requestId,
            @RequestHeader("X-Authenticated-User") String currentUserId
    ) {
        String msg = friendService.acceptRequest(requestId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(msg, null));
    }

    // INTERNAL API (Trusted Access Only for Chat Service)
    @GetMapping("/internal/check-access")
    public boolean internalCheckAccess(
            @RequestParam("userId") String userId,
            @RequestParam("roomId") String roomId
    ) {
        // This endpoint should be protected by InternalFilter in User Service
        return friendService.checkRoomAccess(userId, roomId);
    }

    @PutMapping("/block/{targetId}")
    public ResponseEntity<ApiResponse<String>> blockUser(
            @PathVariable String targetId,
            @RequestHeader("X-Authenticated-User") String currentUserId
    ) {
        friendService.blockUser(currentUserId, targetId);
        return ResponseEntity.ok(ApiResponse.success("User Blocked successfully", null));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<FriendDTO>>> getPending(
            @RequestHeader("X-Authenticated-User") String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Pending Requests", friendService.getPendingRequests(userId)));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<FriendDTO>>> getFriends(
            @RequestHeader("X-Authenticated-User") String userId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Friends List", friendService.getFriendsList(userId)));
    }
}
