package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.oneshop.service.UserService;

import com.oneshop.entity.User;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/api/auth/signup")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password) {

        System.out.println("=== [DEBUG REGISTER] ===");
        System.out.println("Username: " + username);
        System.out.println("Email: " + email);
        System.out.println("Password (raw): " + password);

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email.trim());
        user.setPassword(passwordEncoder.encode(password.trim()));

        userService.register(user, "ROLE_USER");

        System.out.println("✅ User đã đăng ký thành công (đã mã hóa mật khẩu)");
        System.out.println("Password đã mã hóa: " + user.getPassword());
        System.out.println("=========================");

        return "redirect:/verify-otp?type=REGISTRATION&email=" + email;
    }


    @GetMapping("/verify-otp")
    public String verifyOtpForm(@RequestParam String type, @RequestParam String email, Model model) {
        model.addAttribute("type", type);
        model.addAttribute("email", email);
        return "verify-otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String type,
                            @RequestParam String email,
                            @RequestParam String otp) {

        if ("REGISTRATION".equals(type)) {
            userService.activate(email, otp);
            return "redirect:/login";
        } else if ("FORGOT".equals(type)) {
            // ✅ Khi xác minh OTP cho quên mật khẩu thành công,
            // chuyển đến trang đặt lại mật khẩu và truyền luôn OTP qua URL
            return "redirect:/reset-password?email=" + email + "&otp=" + otp;
        }
        return "redirect:/error";
    }

    @GetMapping("/forgot")
    public String forgotForm() {
        return "forgot";
    }

    @PostMapping("/forgot")
    public String forgot(@RequestParam String email) {
        userService.forgotPassword(email);
        return "redirect:/verify-otp?type=FORGOT&email=" + email;
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam String email,
                            @RequestParam(required = false) String otp,  // ✅ fix: không bắt buộc OTP
                            Model model) {
        model.addAttribute("email", email);
        model.addAttribute("otp", otp);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@RequestParam String email,
                        @RequestParam String otp,
                        @RequestParam String newPassword,
                        @RequestParam String confirmPassword) {

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/error";
        }
        userService.resetPassword(email, otp, newPassword);
        return "redirect:/login";
    }
    
}