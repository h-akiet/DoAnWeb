package com.oneshop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor // Tự động inject các dependency (final)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Service để load User từ DB

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Kiểm tra header có tồn tại và đúng định dạng "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Bỏ qua và cho filter tiếp theo chạy
            return;
        }

        // 2. Lấy token từ header (bỏ "Bearer ")
        jwt = authHeader.substring(7);

        // 3. Trích xuất username từ token
        username = jwtService.extractUsername(jwt);

        // 4. Nếu có username VÀ user chưa được xác thực trong context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Tải thông tin user từ DB
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 5. Xác thực token
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Nếu token hợp lệ, tạo object Authentication
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // Không cần credentials (password)
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // 6. Lưu thông tin xác thực vào SecurityContextHolder
                // Spring Security sẽ dùng thông tin này để cho phép truy cập
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // 7. Cho phép request đi tiếp
        filterChain.doFilter(request, response);
    }
}