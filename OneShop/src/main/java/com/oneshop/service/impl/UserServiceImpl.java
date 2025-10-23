package com.oneshop.service.vendor.impl;

import com.oneshop.dto.vendor.ProfileUpdateDto;
import com.oneshop.entity.vendor.User;
import com.oneshop.repository.vendor.UserRepository;
import com.oneshop.service.vendor.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + username));
    }

    @Override
    @Transactional
    public User updateUserProfile(String username, ProfileUpdateDto profileUpdateDto) {
        User user = findByUsername(username);

        // Kiểm tra xem email mới có bị trùng với người khác không
        Optional<User> userByNewEmail = userRepository.findByEmail(profileUpdateDto.getEmail());
        if (userByNewEmail.isPresent() && !userByNewEmail.get().getUsername().equals(username)) {
            throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác.");
        }

        user.setFullName(profileUpdateDto.getFullName());
        user.setEmail(profileUpdateDto.getEmail());
        user.setAddress(profileUpdateDto.getAddress());
        user.setPhoneNumber(profileUpdateDto.getPhoneNumber());

        return userRepository.save(user);
    }
}