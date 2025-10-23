package com.oneshop.service.vendor.impl;

import com.oneshop.dto.vendor.RegisterRequestDto;
import com.oneshop.entity.vendor.Role;
import com.oneshop.entity.vendor.Shop;
import com.oneshop.entity.vendor.User;
import com.oneshop.repository.vendor.UserRepository;
import com.oneshop.service.vendor.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerNewVendor(RegisterRequestDto registerRequestDto) {
        // === THÊM VALIDATION MẬT KHẨU ===
        if (!registerRequestDto.getPassword().equals(registerRequestDto.getConfirmPassword())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp."); 
            // Controller cần bắt lỗi này và trả về form với thông báo
        }
        // ==============================

        User newUser = new User();
        newUser.setUsername(registerRequestDto.getUsername());
        newUser.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        newUser.setEmail(registerRequestDto.getEmail());
        newUser.setFullName(registerRequestDto.getFullName());
        newUser.setPhoneNumber(registerRequestDto.getPhoneNumber()); // === THÊM DÒNG NÀY ===
        newUser.setRole(Role.VENDOR);

        Shop newShop = new Shop();
        newShop.setName(registerRequestDto.getFullName() + "'s Shop");
        newShop.setDescription("Chào mừng đến với cửa hàng của tôi trên OneShop!");
        newShop.setContactEmail(registerRequestDto.getEmail());
        newShop.setContactPhone(registerRequestDto.getPhoneNumber()); // === THÊM DÒNG NÀY ===
        newShop.setUser(newUser);

        newUser.setShop(newShop);

        userRepository.save(newUser);
    }

    @Override
    public boolean isUsernameExist(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    @Override
    public boolean isEmailExist(String email) {
        return userRepository.findByEmail(email).isPresent();
    }
}