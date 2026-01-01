package com.sanchar.user_service.service;
import com.sanchar.user_service.dto.FriendDTO;
import com.sanchar.user_service.model.FriendStatus;
import com.sanchar.user_service.model.Friendship;
import com.sanchar.user_service.model.User;
import com.sanchar.user_service.repository.FriendshipRepository;
import com.sanchar.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendService {

    private final FriendshipRepository friendshipRepo;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;

    private String resolveUserId(String headerValue) {

        if (userRepository.existsById(headerValue)) {
            return headerValue;
        }

        User user = userRepository.findByUsername(headerValue)
                .orElseThrow(() -> new RuntimeException("User identity not found: " + headerValue));
        return user.getId();
    }

    // 1. Send Request
//    public void sendRequest(String requesterId, String targetUserId) {
//        if (requesterId.equals(targetUserId)) throw new RuntimeException("Cannot add self");
//
//        Friendship f = Friendship.builder()
//                .requesterId(requesterId)
//                .addresseeId(targetUserId)
//                .status(FriendStatus.PENDING)
//                .createdAt(LocalDateTime.now())
//                .build();
//        friendshipRepo.save(f);
//    }
    public void sendRequest(String requesterHeader, String targetUserId) {
        // 1. Resolve to ID (Fixes the "luffyj" storage issue)
        String requesterId = resolveUserId(requesterHeader);

        if (requesterId.equals(targetUserId)) throw new RuntimeException("Cannot add self");

        // 2. Fix the DB Query to look for PENDING or ACCEPTED
        // We shouldn't create a new one if PENDING exists.
        Optional<Friendship> existing = friendshipRepo.findByRequesterIdAndAddresseeId(requesterId, targetUserId);

        if (existing.isPresent()) {
            throw new RuntimeException("Request already sent or users are already friends.");
        }

        Friendship f = Friendship.builder()
                .requesterId(requesterId) // Stores ID "695...", NOT "luffyj"
                .addresseeId(targetUserId)
                .status(FriendStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        friendshipRepo.save(f);
    }

//    public String acceptRequest(String requestId, String currentUserId) {
//        Friendship f = friendshipRepo.findById(requestId)
//                .orElseThrow(() -> new RuntimeException("Not Found"));
//
//        if (!f.getAddresseeId().equals(currentUserId)) throw new RuntimeException("Unauthorized");
//
//        f.setStatus(FriendStatus.ACCEPTED);
//
//        // GENERATE CONSISTENT ROOM ID
//        // Logic: "minID_maxID" (Always alphabetical) so User A and B get the same string
//        String u1 = f.getRequesterId();
//        String u2 = f.getAddresseeId();
//        String roomId = (u1.compareTo(u2) < 0) ? u1 + "_" + u2 : u2 + "_" + u1;
//
//        f.setRoomId(roomId);
//        friendshipRepo.save(f);
//
//        // SYNC PERMISSION TO REDIS (The fast layer for Chat Service)
//        String key = "room:access:" + roomId;
//        redisTemplate.opsForSet().add(key, u1, u2);
//        redisTemplate.expire(key, 24, TimeUnit.HOURS); // Refresh on access
//
//        return "Friend Added. Room ID: " + roomId;
//    }
//
    // 4. BLOCK USER (Critical Logic)
    public void blockUser(String currentUserId, String targetUserId) {
        // Find if a relationship exists
        // (We need to check both directions A->B or B->A to find the doc)
        Friendship f = friendshipRepo.findByRequesterIdAndAddresseeId(currentUserId, targetUserId)
                .orElseGet(() -> friendshipRepo.findByRequesterIdAndAddresseeId(targetUserId, currentUserId)
                        .orElse(null));

        if (f == null) {
            // No friendship exists? Create a new one just to say "BLOCKED"
            f = Friendship.builder()
                    .requesterId(currentUserId) // The one doing the blocking
                    .addresseeId(targetUserId)
                    .status(FriendStatus.BLOCKED)
                    .createdAt(LocalDateTime.now())
                    // No roomId needed if they weren't friends yet, but good to null check
                    .build();
        } else {
            // Update existing relationship
            f.setStatus(FriendStatus.BLOCKED);
            // We verify who blocked who (Logic logic logic)
            // Ideally, add a field "blockedBy" to the model, but for MVP:
            // The relationship enters a BLOCKED state.
        }

        friendshipRepo.save(f);

        // 🚨 CRITICAL: NUCLEAR OPTION ON CACHE
        // Remove the Room permission from Redis immediately.
        // The next time they try to chat, it will fallback to DB, see "BLOCKED", and fail.
        if (f.getRoomId() != null) {
            String key = "room:access:" + f.getRoomId();
            redisTemplate.delete(key);
            log.info("Access revoked for Room {}", f.getRoomId());
        }
    }

    public String acceptRequest(String requestId, String headerValue) { // Param renamed for clarity

        // 1. RESOLVE USERNAME TO ID
        String currentUserId = resolveUserId(headerValue);

        Friendship f = friendshipRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Not Found"));

        // 2. NOW COMPARE ID vs ID
        if (!f.getAddresseeId().equals(currentUserId)) {
            throw new RuntimeException("Unauthorized: This request was sent to user ID " + f.getAddresseeId() + ", but you are " + currentUserId);
        }

        // ... Rest of your logic is perfect ...
        f.setStatus(FriendStatus.ACCEPTED);

        String u1 = f.getRequesterId();
        String u2 = f.getAddresseeId();
        String roomId = (u1.compareTo(u2) < 0) ? u1 + "_" + u2 : u2 + "_" + u1;

        f.setRoomId(roomId);
        friendshipRepo.save(f);

        // Redis Sync
        String key = "room:access:" + roomId;
        redisTemplate.opsForSet().add(key, u1, u2);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);

        return "Friend Added. Room ID: " + roomId;
    }
    // 3. Fallback Validation (Called by Chat Service internally)
    public boolean checkRoomAccess(String userId, String roomId) {
        // Find the friendship for this room
        Friendship f = friendshipRepo.findByRoomId(roomId).orElse(null);

        if (f == null) return false;

        // Check 1: Is user actually in this relationship?
        if (!f.getRequesterId().equals(userId) && !f.getAddresseeId().equals(userId)) {
            return false;
        }

        // Check 2: Is the status strictly ACCEPTED?
        // If it is BLOCKED or PENDING, return FALSE.
        return f.getStatus() == FriendStatus.ACCEPTED;
    }

//    public List<FriendDTO> getPendingRequests(String currentUserId) {
//        // Find rows where I am the target and status is PENDING
//        List<Friendship> list = friendshipRepo.findByAddresseeIdAndStatus(currentUserId, FriendStatus.PENDING);
//
//        return list.stream().map(f -> {
//            // Fetch who sent it
//            User sender = userRepository.findById(f.getRequesterId()).orElse(null);
//            return (sender != null) ? mapToDTO(f, sender) : null;
//        }).filter(Objects::nonNull).toList();
//    }

    public List<FriendDTO> getPendingRequests(String currentHeaderValue) {
        // 1. Resolve header (e.g. "zoroisgoat") to ID ("69562...")
        String myUserId = resolveUserId(currentHeaderValue);

        // 2. Query matches ID now!
        List<Friendship> list = friendshipRepo.findByAddresseeIdAndStatus(myUserId, FriendStatus.PENDING);

        return list.stream().map(f -> {
            // Requester stored as ID now, so this lookup works
            User sender = userRepository.findById(f.getRequesterId()).orElse(null);
            return (sender != null) ? mapToDTO(f, sender) : null;
        }).filter(Objects::nonNull).toList();
    }

    // 7. GET ALL FRIENDS (Accepted)
//    public List<FriendDTO> getFriendsList(String currentUserId) {
//        List<Friendship> list = friendshipRepo.findByRequesterIdAndStatusOrAddresseeIdAndStatus(
//                currentUserId, FriendStatus.ACCEPTED,
//                currentUserId, FriendStatus.ACCEPTED
//        );
//
//        return list.stream().map(f -> {
//            // If I am requester, Friend is addressee. Vice versa.
//            String friendId = f.getRequesterId().equals(currentUserId)
//                    ? f.getAddresseeId()
//                    : f.getRequesterId();
//
//            User friend = userRepository.findById(friendId).orElse(null);
//            return (friend != null) ? mapToDTO(f, friend) : null;
//        }).filter(Objects::nonNull).toList();
//    }

    public List<FriendDTO> getFriendsList(String headerValue) {

        // 1. 👇 RESOLVE USERNAME TO ID (CRITICAL FIX)
        String currentUserId = resolveUserId(headerValue);

        // 2. Now query with the valid ID
//        List<Friendship> list = friendshipRepo.findByRequesterIdAndStatusOrAddresseeIdAndStatus(
//                currentUserId, FriendStatus.ACCEPTED,
//                currentUserId, FriendStatus.ACCEPTED
//        );
        List<Friendship> list = friendshipRepo.findAllFriends(currentUserId);

        return list.stream().map(f -> {
            String friendId = f.getRequesterId().equals(currentUserId)
                    ? f.getAddresseeId()
                    : f.getRequesterId();

            User friend = userRepository.findById(friendId).orElse(null);
            return (friend != null) ? mapToDTO(f, friend) : null;
        }).filter(Objects::nonNull).toList();
    }


    // Helper
    private FriendDTO mapToDTO(Friendship f, User user) {
        return FriendDTO.builder()
                .requestId(f.getId()) // To Accept/Reject later
                .friendId(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .profilePicture(user.getProfilePictureUrl())
                .roomId(f.getRoomId()) // Important for Chat!
                .build();
    }


}
