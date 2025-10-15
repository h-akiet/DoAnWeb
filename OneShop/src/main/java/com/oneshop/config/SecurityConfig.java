package com.oneshop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import com.oneshop.service.AuthTokenFilter;
import com.oneshop.service.JwtUtils;
import com.oneshop.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws java.io.IOException {
                // Sinh JWT
                String jwt = jwtUtils.generateJwtToken(authentication);
                Cookie cookie = new Cookie("jwtToken", jwt);
                cookie.setHttpOnly(true);
                cookie.setPath("/");
                cookie.setMaxAge(24 * 60 * 60);
                response.addCookie(cookie);

                System.out.println("✅ JWT token đã được tạo và lưu vào cookie sau đăng nhập thành công!");

                // Chuyển hướng đến /home
                response.sendRedirect("/home");
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Tắt CSRF cho API, giữ session cho form login
            .csrf(csrf -> csrf.disable())

            // Sử dụng session khi cần (cho form login)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // Phân quyền truy cập
            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập công khai
            	.requestMatchers("/assets/**","/", "/webjars/**").permitAll() 
                .requestMatchers("/", "/home", "/login", "/register",
                                "/verify-otp", "/forgot", "/reset-password",
                                "/api/auth/**", "/error", "/search", "/*").permitAll()
                // Cho phép tài nguyên tĩnh và JSP
                .requestMatchers("/WEB-INF/decorators/**",
                                "/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                // Yêu cầu quyền ROLE_SHIPPER cho /shipper/**
                .requestMatchers("/shipper/**").hasAuthority("ROLE_SHIPPER")
                // Các request khác yêu cầu đăng nhập
                .anyRequest().authenticated()
            )

            // Cấu hình form login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login") // URL xử lý POST login từ form
                .successHandler(customSuccessHandler()) // Xử lý thành công với JWT
                .failureUrl("/login?error=true")
                .permitAll()
            )

            // Cấu hình logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            );

        // Thêm authentication provider và JWT filter
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public HttpFirewall allowSlashesFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }
}