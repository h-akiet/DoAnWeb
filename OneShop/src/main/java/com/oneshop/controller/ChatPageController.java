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
     * Mở trang chat giữa USER/SHIPPER ↔ VENDOR
     * URL: /chat/{targetId}
     */
    @GetMapping("/chat/{targetId}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'SHIPPER')")
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
            // Chuyển hướng về Dashboard (hoặc orders) nếu là Vendor/Shipper
            if (currentUser.getRole().getName() == RoleName.VENDOR) {
                return "redirect:/vendor/chat";
            }
            if (currentUser.getRole().getName() == RoleName.SHIPPER) {
                return "redirect:/shipper/orders"; 
            }
            return "redirect:/contact";
        }
        
        RoleName currentUserRole = currentUser.getRole().getName();
        RoleName targetUserRole = targetUser.getRole().getName();

        // LOGIC CHAT: USER/SHIPPER (Chat với VENDOR)
        if (currentUserRole == RoleName.USER || currentUserRole == RoleName.SHIPPER) {
            if (targetUserRole != RoleName.VENDOR) {
                ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể trò chuyện với chủ shop.");
                return "redirect:/contact";
            }
            // Nếu người hiện tại là Shipper, hiển thị giao diện Shipper Chat
            if (currentUserRole == RoleName.SHIPPER) {
                 model.addAttribute("vendor", targetUser); // Dùng vendor làm target
                 return "user/chat_user"; // Dùng chung giao diện chat của User/Shipper
            }
            // Nếu là User
            model.addAttribute("vendor", targetUser);
            return "user/chat_user"; 
        }

        // LOGIC CHAT: VENDOR (Chat với USER/SHIPPER)
        if (currentUserRole == RoleName.VENDOR) {
            if (targetUserRole != RoleName.USER && targetUserRole != RoleName.SHIPPER) {
                ra.addFlashAttribute("errorMessage", "Bạn chỉ có thể trò chuyện với khách hàng hoặc shipper.");
                return "redirect:/vendor/chat";
            }
            model.addAttribute("user", targetUser); // Dùng user làm target (dù là shipper/user)
            return "vendor/chat_vendor"; 
        }

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