package com.oneshop.controller;

import com.oneshop.entity.Role.RoleName;
import com.oneshop.entity.User;
import com.oneshop.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatPageController {

    private final UserService userService;

    public ChatPageController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Mở trang chat giữa USER ↔ VENDOR
     * URL: /chat/{targetId}
     */
    @GetMapping("/chat/{targetId}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR')")
    public String openChat(
            @PathVariable Long targetId,
            Authentication auth,
            Model model,
            RedirectAttributes ra) {

        String currentUsername = auth.getName();
        User currentUser = userService.findByUsername(currentUsername);
        User targetUser = userService.findById(targetId);

        // Kiểm tra người nhận tồn tại
        if (targetUser == null) {
            ra.addFlashAttribute("errorMessage", "Không tìm thấy người dùng.");
            return "redirect:/contact";
        }

        // Không cho phép chat với chính mình
        if (currentUser.getId().equals(targetId)) {
            if (currentUser.getRole().getName() == RoleName.VENDOR) {
                return "redirect:/vendor/chat";
            } else {
                ra.addFlashAttribute("errorMessage", "Không thể chat với chính mình.");
                return "redirect:/contact";
            }
        }

        // Nếu người đăng nhập là VENDOR, kiểm tra targetUser phải là USER
        if (currentUser.getRole().getName() == RoleName.VENDOR) {
            if (targetUser.getRole().getName() == RoleName.VENDOR) {
                ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể trò chuyện với khách hàng, không phải vendor khác.");
                return "redirect:/contact";
            }
            model.addAttribute("user", targetUser);
            return "vendor/chat_vendor"; // Trả về giao diện chat của vendor
        }

        // Nếu người đăng nhập là USER, targetUser phải là VENDOR
        if (currentUser.getRole().getName() == RoleName.USER) {
            if (targetUser.getRole().getName() != RoleName.VENDOR) {
                ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể trò chuyện với chủ shop.");
                return "redirect:/contact";
            }
            model.addAttribute("vendor", targetUser);
            return "user/chat_user"; // Trả về giao diện chat của user
        }

        // Trường hợp không xác định vai trò
        ra.addFlashAttribute("errorMessage", "Không thể mở chat, quyền hạn không hợp lệ.");
        return "redirect:/contact";
    }

    /**
     * Trang danh sách chat của VENDOR
     */
    @GetMapping("/vendor/chat")
    @PreAuthorize("hasRole('VENDOR')")
    public String vendorChatList() {
        return "vendor/chat_vendor";
    }
}