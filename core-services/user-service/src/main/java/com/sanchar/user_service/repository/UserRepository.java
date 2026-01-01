package com.sanchar.user_service.repository;

import com.sanchar.user_service.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User,String> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("{ '$and': [ { '_id': { '$ne': ?0 } }, { '$or': [ { 'username': { '$regex': ?1, '$options': 'i' } }, { 'fullName': { '$regex': ?1, '$options': 'i' } } ] } ] }")
    List<User> searchUsers(String myUserId, String keyword);
}
