//package com.oneshop.controller.admin;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.util.List;
//
//import com.oneshop.entity.User;
//import com.oneshop.service.admin.UserService;
//
//@Controller
//	@RequestMapping("/admin")
//	public class AdminUserController {
//
//	    @Autowired
//	    private UserService userService; 
//
//	    /**
//	     * Xử lý yêu cầu GET /admin/users để hiển thị trang quản lý người dùng.
//	     * Hỗ trợ tìm kiếm theo từ khóa (tên hoặc email).
//	     */
//	    @GetMapping("/users")
//	    public String manageUsers(@RequestParam(value = "keyword", required = false) String keyword, Model model) {
//	        
//	        // 1. Lấy danh sách người dùng
//	        List<User> users;
//	        if (keyword != null && !keyword.trim().isEmpty()) {
//	            // Nếu có từ khóa, gọi service tìm kiếm
//	            users = userService.searchUsers(keyword);
//	            model.addAttribute("keyword", keyword); // Giữ lại từ khóa tìm kiếm trên giao diện
//	        } else {
//	            // Lấy toàn bộ danh sách người dùng
//	            users = userService.getAllUsers();
//	        }
//
//	        // 2. Đưa danh sách người dùng vào Model để Thymeleaf render
//	        model.addAttribute("users", users);
//	        
//	        // 3. Trả về tên template users.html
//	        return "admin/users"; 
//	    }
//
//	    /**
//	     * Xử lý yêu cầu POST để thay đổi Quyền (Role) của người dùng.
//	     */
//	    @PostMapping("/users/change-role/{userId}")
//	    public String changeUserRole(@PathVariable("userId") Integer userId, 
//	                                 @RequestParam("newRole") String newRole,
//	                                 RedirectAttributes redirectAttributes) {
//	        try {
//	            userService.updateUserRole(userId, newRole);
//	            redirectAttributes.addFlashAttribute("successMessage", "Cấp quyền thành công!");
//	        } catch (Exception e) {
//	            redirectAttributes.addFlashAttribute("errorMessage", "Cấp quyền thất bại: " + e.getMessage());
//	        }
//	        // Chuyển hướng về trang quản lý người dùng sau khi hoàn tất
//	        return "redirect:/admin/users";
//	    }
//
//	    /**
//	     * Xử lý yêu cầu GET để Khóa/Mở khóa (Toggle Status) tài khoản người dùng.
//	     */
//	    @GetMapping("/users/toggle-status/{userId}")
//	    public String toggleUserStatus(@PathVariable("userId") Integer userId, RedirectAttributes redirectAttributes) {
//	        try {
//	            userService.toggleUserStatus(userId);
//	            redirectAttributes.addFlashAttribute("successMessage", "Thay đổi trạng thái tài khoản thành công!");
//	        } catch (Exception e) {
//	            redirectAttributes.addFlashAttribute("errorMessage", "Thay đổi trạng thái thất bại: " + e.getMessage());
//	        }
//	        // Chuyển hướng về trang quản lý người dùng sau khi hoàn tất
//	        return "redirect:/admin/users";
//	    }
//
//	}
