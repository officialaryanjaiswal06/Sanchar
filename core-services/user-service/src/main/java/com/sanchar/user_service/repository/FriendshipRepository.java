package com.sanchar.user_service.repository;

import com.sanchar.user_service.model.FriendStatus;
import com.sanchar.user_service.model.Friendship;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends MongoRepository<Friendship, String> {

    Optional<Friendship> findByRequesterIdAndAddresseeId(String requesterId, String addresseeId);

    // Required for checking access by Room ID
    Optional<Friendship> findByRoomId(String roomId);

    boolean existsByRoomIdAndRequesterIdOrRoomIdAndAddresseeId(String rid1, String u1, String rid2, String u2);

    List<Friendship> findByAddresseeIdAndStatus(String userId, FriendStatus status);

    List<Friendship> findByRequesterIdAndStatusOrAddresseeIdAndStatus(
            String u1, FriendStatus s1,
            String u2, FriendStatus s2
    );
    @Query("{ '$or': [ { 'requesterId': ?0, 'status': 'ACCEPTED' }, { 'addresseeId': ?0, 'status': 'ACCEPTED' } ] }")
    List<Friendship> findAllFriends(String userId);
}
