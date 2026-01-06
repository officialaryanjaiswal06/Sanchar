package com.sanchar.chat_service.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageAck implements Serializable {
    private String messageId;
    private String roomId;

    private String senderId;
    private String recipientId;

    private MessageStatus status;
}
