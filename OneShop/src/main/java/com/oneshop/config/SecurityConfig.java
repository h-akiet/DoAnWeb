package com.oneshop.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import com.oneshop.service.AuthTokenFilter;
import com.oneshop.service.JwtUtils;
import com.oneshop.service.UserService;

// KHÔNG cần import các class inner hay http/cookie nữa
// vì logic đó đã được chuyển ra file riêng

@Configuration
public class SecurityConfig {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;
    
    // [SỬA 1] - Tiêm BEAN thành công (từ file CustomSuccessHandler.java)
    @Autowired
    private CustomSuccessHandler customSuccessHandler;

    // [SỬA 2] - Tiêm BEAN thất bại (từ file CustomAuthenticationFailureHandler.java)
    @Autowired
    private CustomAuthenticationFailureHandler customFailureHandler;

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

   
    private static final String[] PUBLIC_URLS = {
            // Tài nguyên tĩnh
            "/assets/**",
            "/webjars/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/static/**",

            // Các trang công khai
            "/",
            "/home",
            "/search",
            "/error",
            "/product/**", // Cho phép xem chi tiết sản phẩm

            // Quy trình xác thực
            "/login",
            "/register",
            "/verify-otp",
            "/forgot",
            "/reset-password",
            "/api/auth/**" // API đăng nhập/đăng ký để lấy token
        };

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable()) 
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(PUBLIC_URLS).permitAll()
                    .requestMatchers("/shipper/**").hasAuthority("ROLE_SHIPPER")
                    .requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                    .anyRequest().authenticated()
                )

                // Cấu hình form login
                .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .usernameParameter("account")
                    .passwordParameter("password")
                    // [SỬA 4] - Sử dụng các biến đã tiêm (inject)
                    .successHandler(customSuccessHandler) 
                    .failureHandler(customFailureHandler)
                    .permitAll()
                )
                
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "jwtToken")
                );

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

    // [SỬA 5] - XÓA BỎ TOÀN BỘ 2 INNER CLASS
    // (Vì chúng đã được chuyển thành các file @Component riêng)
    
    /*
    public static class CustomAuthenticationSuccessHandler implements ... {
        ...
    }
    */
    
    /*
    public static class CustomAuthenticationFailureHandler implements ... {
        ...
    }
    */
}