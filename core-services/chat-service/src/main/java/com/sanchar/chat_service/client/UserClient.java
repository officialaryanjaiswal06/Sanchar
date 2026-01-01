package com.sanchar.chat_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/friends/internal/check-access")
    boolean checkAccess(@RequestParam("userId") String userId, @RequestParam("roomId") String roomId);
}
