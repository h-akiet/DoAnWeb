package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.oneshop.entity.Order;
import com.oneshop.entity.User;
import com.oneshop.service.OrderService;
import com.oneshop.service.ReviewService;

import java.util.ArrayList; // <-- THÊM IMPORT
import java.util.HashMap;   // <-- THÊM IMPORT
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/user/orders")
public class UserOrdersController {

    @Autowired
    private OrderService orderService;
    
    @Autowired
    private ReviewService reviewService;
    private static final Logger log = LoggerFactory.getLogger(UserOrdersController.class);

    @GetMapping
    public String listOrders(@AuthenticationPrincipal User user, Model model) {
        String username = user.getUsername(); 
    	List<Order> userOrders = orderService.findOrdersByCurrentUser(username); 

        model.addAttribute("orders", userOrders);
        return "user/orders"; 
    }
    
    @PostMapping("/{id}/cancel")
    @ResponseBody // <-- Thêm ResponseBody
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
        
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Vui lòng đăng nhập."));
        }
        
        try {
            orderService.cancelOrder(id, user.getUsername());
            
            // Trả về thông báo thành công
            return ResponseEntity.ok(Map.of("message", "Đơn hàng #" + id + " đã được hủy thành công."));
            
        } catch (Exception e) {
            // Bắt lỗi nếu service ném ra (ví dụ: đơn hàng không tìm thấy, không thể hủy)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    // Xử lý GET chi tiết đơn hàng (Dùng cho AJAX Modal)
    @GetMapping("/{id}/details")
    public String getOrderDetails(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        
        // FIX: Lấy username và truyền vào service để kiểm tra quyền sở hữu
        String username = user.getUsername();
        Order order = orderService.findOrderByIdAndUser(id, username); 
        
        model.addAttribute("order", order);
        
        return "user/order-details-fragment :: content";
    }
    @GetMapping("/{orderId}/reviewed/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Boolean>> checkReviewed(
            @PathVariable Long orderId,
            @PathVariable Long productId,
            @AuthenticationPrincipal User user) {

        try {
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            Order order = orderService.findOrderByIdAndUser(orderId, user.getUsername());
            if (order == null) {
                // Không phải đơn hàng của user, hoặc không tồn tại
                return ResponseEntity.ok(Map.of("reviewed", false));
            }

            // Gọi hàm service mà bạn đã cung cấp
            boolean reviewed = reviewService.isProductReviewed(orderId, productId);
            
            return ResponseEntity.ok(Map.of("reviewed", reviewed));

        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra review cho orderId={} và productId={}: {}", orderId, productId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}