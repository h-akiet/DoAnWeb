package com.oneshop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.oneshop.service.vendor.impl.CustomOAuth2UserService;
import com.oneshop.service.vendor.impl.UserDetailsServiceImpl;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // === THÊM BEAN NÀY ===
    @Bean
    public CustomOAuth2UserService oauth2UserService() {
        return new CustomOAuth2UserService();
    }
    // ======================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                // Cho phép truy cập các trang/tài nguyên này mà không cần đăng nhập
                .requestMatchers("/", "/assets/**", "/uploads/images/**", "/register", "/login", "/oauth2/**").permitAll() 
                .requestMatchers("/vendor/**").hasRole("VENDOR")
                .anyRequest().authenticated() 
            )
            .formLogin(form -> form // Cấu hình đăng nhập bằng form
                .loginPage("/login") 
                .defaultSuccessUrl("/vendor/dashboard", true)
                .permitAll()
            )
            // === THÊM CẤU HÌNH OAUTH2 LOGIN ===
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login") // Vẫn dùng trang login tùy chỉnh
                .defaultSuccessUrl("/vendor/dashboard", true) // Đi đến dashboard sau khi login OAuth2 thành công
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService()) // Sử dụng service tùy chỉnh
                )
            )
            // =================================
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout") 
                .permitAll()
            );

        return http.build();
    }
}