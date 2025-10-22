package com.oneshop.controller;

import java.math.BigDecimal; 
// Các import không cần thiết đã bị xóa bớt
import java.nio.charset.StandardCharsets; 
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.oneshop.config.VnPayConfig; // Đảm bảo import đúng
import com.oneshop.dto.PlaceOrderRequest; 
import com.oneshop.entity.Order;
import com.oneshop.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    // [SỬA LỖI 1] Tiêm (Inject) bean VnPayConfig vào
    @Autowired
    private VnPayConfig vnPayConfig;

    @PostMapping("/placeOrder")
    @ResponseBody 
    public ResponseEntity<?> placeOrder(
            @RequestBody PlaceOrderRequest orderRequest, 
            HttpServletRequest request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập để tiếp tục."));
        }
        String username = principal.getName();
        
        Order newOrder;
        try {
            newOrder = orderService.createOrderFromRequest(username, orderRequest);
            
            if (newOrder == null) {
                 throw new RuntimeException("Không thể tạo đơn hàng do lỗi không xác định.");
            }

        } catch (Exception e) {
            // e.printStackTrace(); 
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        Map<String, Object> jsonResponse = new HashMap<>();

        if ("cod".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            jsonResponse.put("status", "success");
            jsonResponse.put("redirectUrl", "/order/success"); 
            return ResponseEntity.ok(jsonResponse);

        } else if ("bank_transfer".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            
            // 1. Kiểm tra tổng tiền
            if (newOrder.getTotal() == null || newOrder.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tổng tiền của đơn hàng không hợp lệ."));
            }
            
            // 2. Lấy các tham số cần thiết
            long amount = newOrder.getTotal()
                                  .multiply(BigDecimal.valueOf(100))
                                  .longValue(); 
            String vnp_TxnRef = newOrder.getId().toString();
            
            // [SỬA LỖI 2] - Gọi phương thức từ bean đã tiêm (instance method)
            String vnp_IpAddr = vnPayConfig.getIpAddress(request);
            
            String vnp_OrderInfo = "Thanh toan don hang " + vnp_TxnRef; 

            // 3. Gọi hàm tạo URL từ bean
            try {
                // [SỬA LỖI 3] - Gọi phương thức từ bean đã tiêm (instance method)
                String paymentUrl = vnPayConfig.createPaymentUrl(
                    vnp_TxnRef, 
                    amount, 
                    vnp_OrderInfo, 
                    vnp_IpAddr
                );
                
                jsonResponse.put("status", "pending_payment");
                jsonResponse.put("paymentUrl", paymentUrl);
                return ResponseEntity.ok(jsonResponse);

            } catch (Exception e) {
                 e.printStackTrace(); // In log lỗi chi tiết
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                      .body(Map.of("message", "Lỗi khi tạo URL thanh toán VNPAY."));
            }
        }

        return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán không được hỗ trợ."));
    }
}