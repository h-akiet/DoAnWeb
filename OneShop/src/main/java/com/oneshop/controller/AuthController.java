package com.oneshop.controller;

import com.oneshop.dto.vendor.RegisterRequestDto;
import com.oneshop.service.vendor.AuthService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.validation.BindingResult;


import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.oneshop.service.UserService;
import com.oneshop.entity.User;
import com.oneshop.service.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
  // Trung 
  private AuthService authService;

    // Trang đăng nhập
    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login"; // Trả về template login.html
    }

    // Trang đăng ký
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequestDto", new RegisterRequestDto()); // Gửi DTO rỗng để bind form
        return "auth/register"; // Trả về template register.html
    }

    // Xử lý đăng ký
    @PostMapping("/register")
    public String registerVendor(@Valid @ModelAttribute("registerRequestDto") RegisterRequestDto registerRequestDto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register"; // Quay lại form nếu có lỗi
        }
        
        // Kiểm tra xem username hoặc email đã tồn tại chưa
        if (authService.isUsernameExist(registerRequestDto.getUsername())) {
            bindingResult.rejectValue("username", "error.registerRequestDto", "Tên đăng nhập đã tồn tại.");
            return "auth/register";
        }
        if (authService.isEmailExist(registerRequestDto.getEmail())) {
            bindingResult.rejectValue("email", "error.registerRequestDto", "Email đã được sử dụng.");
            return "auth/register";
        }

        authService.registerNewVendor(registerRequestDto);
        redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
  //kiet
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private AuthenticationManager authenticationManager;
   
   


    @PostMapping("/api/auth/signup")
    public ResponseEntity<Map<String, Object>> register(@RequestParam String fullName,
                                                        @RequestParam String email,
                                                        @RequestParam String password,
                                                        @RequestParam String confirmPassword) {
        Map<String, Object> response = new HashMap<>();
        
        // Kiểm tra mật khẩu khớp (client-side cũng có, nhưng server-side bắt buộc)
        if (!password.equals(confirmPassword)) {
            response.put("success", false);
            response.put("message", "Mật khẩu và xác nhận không khớp!");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Tạo user và gọi service (sẽ kiểm tra tồn tại + gửi OTP)
            User user = new User();
            user.setUsername(fullName.trim());
      
            user.setEmail(email.trim());
            user.setPassword(passwordEncoder.encode(password.trim()));

            userService.register(user, "USER"); // Service sẽ throw nếu email tồn tại hoặc gửi OTP fail

            response.put("success", true);
            response.put("redirect", "/verify-otp?type=REGISTRATION&email=" + email);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Email đã tồn tại
            response.put("success", false);
            response.put("message", "Email đã tồn tại! Vui lòng dùng email khác.");
            return ResponseEntity.badRequest().body(response);
        } catch (RuntimeException e) {
            // Lỗi gửi OTP (hoặc khác từ service)
            response.put("success", false);
            response.put("message", "Không thể gửi OTP! Vui lòng kiểm tra email và thử lại.");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            // Lỗi chung
            response.put("success", false);
            response.put("message", "Có lỗi xảy ra trong quá trình đăng ký. Vui lòng thử lại!");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 🔹 Trang xác thực OTP
    @GetMapping("/verify-otp")
    public String verifyOtpForm(@RequestParam String type, @RequestParam String email, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("email", email);
        return "guest/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String type,
                            @RequestParam String email,
                            @RequestParam String otp,
                            Model model) {
        try {
            if ("REGISTRATION".equals(type)) {
                userService.activate(email, otp);
                model.addAttribute("message", "Xác thực thành công! Hãy đăng nhập.");
                return "guest/index"; 
            } else if ("FORGOT".equals(type)) {
                return "redirect:/reset-password?email=" + email + "&otp=" + otp;
            } else {
                model.addAttribute("error", "Loại xác thực không hợp lệ!");
                return "guest/verify-otp";
            }
        } catch (RuntimeException e) {
            // Bắt lỗi "Invalid OTP" hoặc các lỗi khác từ service
            model.addAttribute("type", type);
            model.addAttribute("email", email);
            model.addAttribute("error", e.getMessage()); 
            return "guest/verify-otp"; // hiển thị lại trang với thông báo lỗi
        }
    }


    // 🔹 Trang quên mật khẩu
    @GetMapping("/forgot")
    public String forgotForm() {
        return "guest/forgot";
    }

    @PostMapping("/forgot")
    public String forgot(@RequestParam String email) {
        userService.forgotPassword(email);
        return "redirect:/verify-otp?type=FORGOT&email=" + email;
    }

    // 🔹 Trang đặt lại mật khẩu
    @GetMapping("/reset-password")
    public String resetForm(@RequestParam String email,
                            @RequestParam(required = false) String otp,
                            Model model) {
        model.addAttribute("email", email);
        model.addAttribute("otp", otp);
        return "guest/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String email,
                        @RequestParam String otp,
                        @RequestParam String newPassword,
                        @RequestParam String confirmPassword,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu mới và xác nhận không khớp!");
            model.addAttribute("email", email);
            model.addAttribute("otp", otp);
            return "guest/reset-password";
        }

        try {
            userService.resetPassword(email, otp, newPassword);
            return "redirect:/login";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/verify-otp?type=FORGOT&email=" + email;
        }
      // kiệt
    }
}