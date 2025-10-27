// src/main/java/com/oneshop/controller/ChatController.java
package com.oneshop.controller;

import com.oneshop.entity.ChatMessageEntity;
import com.oneshop.model.ChatMessage;
import com.oneshop.repository.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessage msg) {
        // Lưu vào CSDL
        chatMessageRepository.save(ChatMessageEntity.builder()
                .sender(msg.getSender())
                .receiver(msg.getReceiver())
                .content(msg.getContent())
                .timestamp(LocalDateTime.now())
                .type(ChatMessageEntity.MessageType.CHAT)
                .build());

        // Gửi đến người nhận
        messagingTemplate.convertAndSendToUser(msg.getReceiver(), "/queue/private", msg);
    }

    @MessageMapping("/chat.join")
    public void join(@Payload ChatMessage msg) {
        msg.setType("JOIN");
        messagingTemplate.convertAndSendToUser(msg.getReceiver(), "/queue/private", msg);
    }
}