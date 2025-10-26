package com.oneshop.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.oneshop.entity.Role;
import com.oneshop.entity.User;
import com.oneshop.repository.OtpRepository; // Thêm import
import com.oneshop.repository.RoleRepository;
import com.oneshop.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OtpRepository otpRepository; // Thêm OtpRepository

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpService otpService;

    @Transactional(rollbackFor = Exception.class)
    public void register(User user, String roleName) {
        if (userRepository.existsByUsername(user.getUsername()) || userRepository.existsByEmail(user.getEmail())) {
            logger.warn("Email hoặc username đã tồn tại: {}", user.getEmail());
            throw new IllegalArgumentException("Email hoặc username đã tồn tại!");
        }

        // Mã hóa mật khẩu nếu chưa mã hóa
        if (!user.getPassword().startsWith("$2a$") && !user.getPassword().startsWith("$2b$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        Role roleUser = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Set<Role> roles = new HashSet<>();
        roles.add(roleUser);
        user.setRoles(roles);
        user.setActivated(false);
        userRepository.save(user);

        // Gửi OTP - Nếu thất bại, rollback toàn bộ
        try {
            otpService.generateAndSendOtp(user.getEmail(), "REGISTRATION");
            logger.info("Đăng ký thành công và gửi OTP cho: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Lỗi gửi OTP cho email {}: {}", user.getEmail(), e.getMessage());
            throw new RuntimeException("Không thể gửi OTP: " + e.getMessage());
        }
    }

    public void activate(String email, String otp) {
        otpService.verifyOtp(email, otp, "REGISTRATION");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActivated(true);
        userRepository.save(user);
        logger.info("Kích hoạt tài khoản thành công: {}", email);
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        otpService.generateAndSendOtp(email, "FORGOT");
        logger.info("Gửi OTP quên mật khẩu thành công: {}", email);
    }

    public void resetPassword(String email, String otp, String newPassword) {
        otpService.verifyOtp(email, otp, "FORGOT");
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Reset mật khẩu thành công: {}", email);
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

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void save(User user) {
        userRepository.save(user);
    }

    public void deleteByEmail(String email) {
        userRepository.findByEmail(email)
                .ifPresent(userRepository::delete);
    }

    public void sendOtpForRegistration(String email) {
        otpService.generateAndSendOtp(email, "REGISTRATION");
    }

    // 🔹 Scheduler xóa user chưa kích hoạt có OTP hết hạn
    @Scheduled(fixedRate = 300000) // Chạy mỗi 10 phút
    @Transactional
    public void deleteExpiredUnverifiedUsers() {
        LocalDateTime now = LocalDateTime.now();
        otpRepository.findByTypeAndExpiresAtBefore("REGISTRATION", now)
                .forEach(otp -> {
                    User user = otp.getUser();
                    if (!user.isActivated()) {
                        logger.info("Xóa tài khoản chưa kích hoạt: {}", user.getEmail());
                        userRepository.delete(user);
                        otpRepository.delete(otp);
                    }
                });
    }

}