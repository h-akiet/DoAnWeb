package com.oneshop.config; // (Hoặc package config của bạn)

import com.oneshop.service.JwtUtils; // Đảm bảo import JwtUtils
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component; // <-- Rất quan trọng

import java.io.IOException;

@Component // <-- Đánh dấu đây là một Bean
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private JwtUtils jwtUtils; // Tiêm JwtUtils

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        Authentication authentication) 
            throws IOException, ServletException {
        
        // Đây là logic bạn đã viết (lấy từ inner class cũ)
        String jwt = jwtUtils.generateJwtToken(authentication);
        Cookie cookie = new Cookie("jwtToken", jwt);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 1 ngày
        response.addCookie(cookie);

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            response.setContentType("application/json");
            // Sửa lại redirect về trang chủ (hoặc trang bạn muốn)
            response.getWriter().write("{\"success\": true, \"redirect\": \"/\"}"); 
        } else {
            // Sửa lại redirect về trang chủ
            response.sendRedirect("/");
        }
    }
}