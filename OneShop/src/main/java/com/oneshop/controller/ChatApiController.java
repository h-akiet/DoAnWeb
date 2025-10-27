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

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ChatMessageRepository chatMessageRepository;

    // DTO rút gọn chỉ chứa thông tin cần cho danh sách chat
    public record ChatUserDto(Long id, String username, String fullName, String email) {}

    @GetMapping("/users")
    public List<ChatUserDto> getChatUsers(Authentication authentication) {
        // Lấy username của vendor hiện tại
        String currentUsername = authentication.getName();

        // Lấy vai trò USER
        var userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy role USER"));

        // Lấy danh sách username của người dùng đã nhắn tin với vendor
        List<String> chatParticipants = chatMessageRepository.findChatParticipants(currentUsername);

        // Lấy thông tin người dùng có vai trò USER và nằm trong danh sách chatParticipants
        return userRepository.findByRoleAndUsernameIn(userRole, chatParticipants)
                .stream()
                .map(u -> new ChatUserDto(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getEmail()
                ))
                .toList();
    }
}