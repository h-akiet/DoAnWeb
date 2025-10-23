package com.oneshop.controller;

import com.oneshop.dto.vendor.RegisterRequestDto;
import com.oneshop.service.vendor.AuthService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    @Autowired
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
    }
}