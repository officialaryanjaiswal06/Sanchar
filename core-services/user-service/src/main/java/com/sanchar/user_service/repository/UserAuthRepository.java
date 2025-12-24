package com.sanchar.user_service.repository;

import com.sanchar.user_service.model.UserAuth;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserAuthRepository extends MongoRepository<UserAuth,String> {
    Optional<UserAuth> findByUserId(String userId);
    Optional<UserAuth> findByRefreshTokenId(String refreshTokenId);
}
