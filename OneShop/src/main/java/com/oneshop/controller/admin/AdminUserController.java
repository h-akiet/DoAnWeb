package com.oneshop.controller.admin; // Đảm bảo đúng package

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger; // Thêm import Logger
import org.slf4j.LoggerFactory; // Thêm import LoggerFactory

import java.util.List;
import java.util.NoSuchElementException; // Import exception phù hợp

import com.oneshop.entity.User;
// import com.oneshop.service.admin.UserService; // Sử dụng UserService interface chính
import com.oneshop.service.UserService; // Sử dụng UserService interface chính

@Controller
@RequestMapping("/admin") // Base path cho admin
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class); // Thêm logger

    // Sử dụng UserService interface chính thay vì UserService admin riêng
    @Autowired
    private UserService userService;

    /**
     * Xử lý yêu cầu GET /admin/users để hiển thị trang quản lý người dùng.
     * Hỗ trợ tìm kiếm theo từ khóa (tên đăng nhập hoặc email).
     */
    @GetMapping("/users")
    public String manageUsers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
        logger.debug("Accessing user management page. Keyword: {}", keyword);
        model.addAttribute("currentPage", "admin-users"); // Đặt currentPage cho layout

        try {
            List<User> users;
            // TODO: Bổ sung phương thức tìm kiếm và lấy tất cả user vào UserService interface và implementation
            // if (keyword != null && !keyword.trim().isEmpty()) {
            //     // Nếu có từ khóa, gọi service tìm kiếm (cần tạo phương thức này)
            //     // users = userService.searchUsers(keyword);
            //     users = List.of(); // Placeholder
            //     model.addAttribute("keyword", keyword); // Giữ lại từ khóa tìm kiếm
            //     logger.debug("Searching users with keyword: {}", keyword);
            // } else {
            //     // Lấy toàn bộ danh sách người dùng (cần tạo phương thức này)
            //     // users = userService.getAllUsers();
            //     users = List.of(); // Placeholder
            //     logger.debug("Fetching all users.");
            // }

            // Tạm thời lấy tất cả Users (Cần có phương thức findAll() trong UserRepository)
             // users = userService.findAll(); // Giả định userService có hàm này hoặc userRepository
             // Hoặc bạn cần inject UserRepository vào đây để lấy users
             users = List.of(); // Placeholder - BẠN CẦN THAY THẾ BẰNG LOGIC LẤY USER THỰC TẾ


            model.addAttribute("users", users);
            return "admin/users"; // Trả về template admin/users.html
        } catch (Exception e) {
             logger.error("Error accessing user management page: {}", e.getMessage(), e);
             model.addAttribute("errorMessage", "Không thể tải danh sách người dùng.");
             model.addAttribute("users", List.of()); // Trả về danh sách rỗng
             return "admin/users";
        }
    }

    /**
     * Xử lý yêu cầu POST để thay đổi Quyền (Role) của người dùng.
     */
    @PostMapping("/users/change-role/{userId}")
    public String changeUserRole(@PathVariable("userId") Long userId, // Đổi sang Long
                                 @RequestParam("newRoleName") String newRoleName, // Đảm bảo tên khớp với form
                                 RedirectAttributes redirectAttributes) {
        logger.info("Attempting to change role for userId: {} to {}", userId, newRoleName);
        try {
            // TODO: Bổ sung phương thức updateUserRole vào UserService interface và implementation
            // userService.updateUserRole(userId, newRoleName);
             logger.warn("UserService.updateUserRole is not fully implemented yet."); // Thông báo logic chưa hoàn chỉnh
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu thay đổi quyền (chức năng đang phát triển)!"); // Thông báo tạm
        } catch (NoSuchElementException e) { // Bắt lỗi cụ thể nếu user/role không tồn tại
             logger.error("Error changing role for userId {}: {}", userId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) { // Bắt lỗi chung khác
            logger.error("Unexpected error changing role for userId {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Cấp quyền thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users"; // Chuyển hướng về trang quản lý
    }

    /**
     * Xử lý yêu cầu GET để Khóa/Mở khóa (Toggle Status) tài khoản người dùng.
     */
    @GetMapping("/users/toggle-status/{userId}")
    public String toggleUserStatus(@PathVariable("userId") Long userId, // Đổi sang Long
                                   RedirectAttributes redirectAttributes) {
         logger.info("Attempting to toggle status for userId: {}", userId);
        try {
            // TODO: Bổ sung phương thức toggleUserStatus vào UserService interface và implementation
            // userService.toggleUserStatus(userId);
             logger.warn("UserService.toggleUserStatus is not fully implemented yet."); // Thông báo logic chưa hoàn chỉnh
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu thay đổi trạng thái (chức năng đang phát triển)!"); // Thông báo tạm
        } catch (NoSuchElementException e) { // Bắt lỗi user không tồn tại
             logger.error("Error toggling status for userId {}: {}", userId, e.getMessage());
             redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) { // Bắt lỗi chung khác
            logger.error("Unexpected error toggling status for userId {}: {}", userId, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Thay đổi trạng thái thất bại: " + e.getMessage());
        }
        return "redirect:/admin/users"; // Chuyển hướng về trang quản lý
    }
}