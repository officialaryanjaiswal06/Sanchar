package com.sanchar.chat_service.controller;
import com.sanchar.chat_service.model.ChatBucket;
import com.sanchar.chat_service.model.ChatMessage;
import com.sanchar.chat_service.model.Conversation;
import com.sanchar.common_library.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatHistoryController {
    private final MongoTemplate mongoTemplate;

//    @GetMapping("/{roomId}")
    @GetMapping("/history/{roomId}")
    public ResponseEntity<ApiResponse<List<ChatMessage>>> getChatHistory(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page // For future pagination logic
    ) {
        // 1. Fetch the Buckets (Latest ones first)
        Query query = Query.query(Criteria.where("rid").is(roomId))
                .with(Sort.by(Sort.Direction.DESC, "_id")) // Uses your Compound Index
                .limit(2); // Fetch last 2 buckets (approx 100-200 messages)

        List<ChatBucket> buckets = mongoTemplate.find(query, ChatBucket.class);

        // 2. Flatten the Buckets into a single List of Messages
        // The bucket order is DESC (newest), so we need to reverse logical processing
        // to show chronological history (Old -> New)
        List<ChatMessage> flatHistory = buckets.stream()
                .flatMap(bucket -> bucket.getMessages().stream())
                .sorted((m1, m2) -> m1.getTimestamp().compareTo(m2.getTimestamp()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("History fetched", flatHistory));
    }

    @GetMapping("/inbox/{userId}")
    public ResponseEntity<ApiResponse<List<Conversation>>> getInbox(@PathVariable String userId) {
        Query query = Query.query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "lastMessageTime"));

        List<Conversation> inbox = mongoTemplate.find(query, Conversation.class);

        return ResponseEntity.ok(ApiResponse.success("Inbox", inbox));
    }
}
