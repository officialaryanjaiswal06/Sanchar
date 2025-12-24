package com.sanchar.file_service.client;

import com.sanchar.common_library.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {
    @PutMapping("/users/{userId}/profile-picture")
    ApiResponse<Void> updateProfilePic(@PathVariable("userId") String userId, @RequestBody Map<String, String> body);
}
