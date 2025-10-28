// src/main/java/com/oneshop/service/UserService.java
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
//import cũ--------
import com.oneshop.dto.ProfileUpdateDto;
import com.oneshop.entity.Role.RoleName;
//import cũ--------
import com.oneshop.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService; // <<< KẾ THỪA
import java.util.Optional;


public interface UserService extends UserDetailsService {

    // --- Các chức năng quản lý User ---
    void register(User user, RoleName roleNameEnum);
    void activate(String email, String otp);
    void forgotPassword(String email);
    void resetPassword(String email, String otp, String newPassword);
    void verifyOtp(String email, String otp, String type);

    User findByUsername(String username);
    Optional<User> findUserByEmail(String email);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    void save(User user);
    void deleteByEmail(String email);

    void sendOtpForRegistration(String email);

    User updateUserProfile(String username, ProfileUpdateDto profileUpdateDto);
    
    User findById(Long id);
    
}