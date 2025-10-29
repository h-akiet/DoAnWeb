package com.oneshop.controller;

import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.repository.ChatMessageRepository;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    public record ChatUserDto(Long id, String username, String fullName, String email) {}

    @GetMapping("/users")
    public List<ChatUserDto> getChatUsers(Authentication authentication) {
        String currentUsername = authentication.getName();

        // 1. Lấy danh sách username của tất cả người dùng đã nhắn tin với vendor/shipper
        List<String> chatParticipants = chatMessageRepository.findChatParticipants(currentUsername);

        // Lọc chính người dùng hiện tại khỏi danh sách tham gia chat
        List<String> targetUsernames = chatParticipants.stream()
            .filter(name -> name != null && !name.equals(currentUsername))
            .toList();

        // 2. Lấy thông tin người dùng dựa trên danh sách username đó (Không cần lọc Role)
        // Chúng ta sẽ giả định UserRepository có hàm findByUsernameIn(List<String>)
        List<User> targetUsers = userRepository.findAll().stream()
            .filter(u -> targetUsernames.contains(u.getUsername()) && u.isActivated())
            .toList();


        return targetUsers.stream()
                .map(u -> new ChatUserDto(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail()
                ))
                .toList();
    }
}