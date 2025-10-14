package com.oneshop.service;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    public void register(User user, String roleName) {
        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("User already exists");
        }

        // Mã hóa mật khẩu nếu chưa mã hóa
        if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        user.setActivated(false);
        userRepository.save(user);

        otpService.generateAndSendOtp(user.getEmail(), "REGISTRATION");
    }

    public void activate(String email, String otp) {
        otpService.verifyOtp(email, otp, "REGISTRATION");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActivated(true);
        userRepository.save(user);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        otpService.generateAndSendOtp(email, "FORGOT");
    }

    public void resetPassword(String email, String otp, String newPassword) {
        // Xác thực OTP lại để đảm bảo đúng người
        otpService.verifyOtp(email, otp, "FORGOT");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void verifyOtp(String email, String otp, String type) {
        otpService.verifyOtp(email, otp, type);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
}
