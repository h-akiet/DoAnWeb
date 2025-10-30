// src/main/java/com/oneshop/controller/OrderController.java
package com.oneshop.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller; // <<< Đổi thành @Controller
import org.springframework.web.bind.annotation.GetMapping; // <<< THÊM IMPORT NÀY
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
// Bỏ @RestController nếu Controller này xử lý cả View

import com.oneshop.config.VnPayConfig; // Đảm bảo import đúng
import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderStatus;
import com.oneshop.service.OrderService;

import org.springframework.ui.Model;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller // <<< Đổi thành @Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private VnPayConfig vnPayConfig;
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    
    @GetMapping("/vnpay_return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        
        // 1. Lấy tất cả tham số VNPAY
        Map<String, String> vnp_Params = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldName.equals("vnp_SecureHash")) {
                vnp_Params.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");

        // 2. Kiểm tra tính hợp lệ của dữ liệu (Checksum)
        if (vnPayConfig.validateVnPayHash(vnp_Params, vnp_SecureHash)) {
            
            String vnp_ResponseCode = request.getParameter("vnp_ResponseCode");
            String vnp_TxnRef = request.getParameter("vnp_TxnRef"); // Order ID
            
            if ("00".equals(vnp_ResponseCode)) {
                // THANH TOÁN THÀNH CÔNG
                try {
                    Long orderId = Long.parseLong(vnp_TxnRef);
                  
                    // ===>>> LOGIC THAY ĐỔI <<<===
                    
                    // 3. Lấy thông tin đơn hàng để tìm shopId
                    // (Giả định bạn có hàm getOrderById trong OrderService)
                    Order order = orderService.getOrderById(orderId);
                    if (order == null || order.getShop() == null) {
                        logger.error("VNPAY success, but Order {} or its Shop is null.", orderId);
                        throw new EntityNotFoundException("Không tìm thấy đơn hàng hoặc Shop liên kết.");
                    }
                    
                    Long shopId = order.getShop().getId();

                    // 4. Gọi hàm updateOrderStatus với shopId đã tìm được
                    orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED, shopId); 

                    // ===>>> KẾT THÚC THAY ĐỔI <<<===

                    model.addAttribute("message", "Thanh toán thành công. Đơn hàng #" + orderId + " đã được xác nhận.");
                    model.addAttribute("orderId", orderId); // Sửa lỗi: Thêm addAttribute(String, Long)
                    return "user/order-success"; // Trang thông báo thành công cho VNPAY
                    
                } catch (Exception e) {
                    logger.error("Error confirming order {} after VNPAY success: {}", vnp_TxnRef, e.getMessage());
                    model.addAttribute("message", "Thanh toán thành công nhưng lỗi xử lý đơn hàng. Vui lòng liên hệ hỗ trợ.");
                    return "user/vnpay-error";
                }
            } else {
                // THANH TOÁN THẤT BẠI
                logger.warn("VNPAY payment failed for Order ID: {}. Response code: {}", vnp_TxnRef, vnp_ResponseCode);
                model.addAttribute("message", "Thanh toán thất bại. Mã lỗi VNPAY: " + vnp_ResponseCode);
                return "user/vnpay-error";
            }
        } else {
            // LỖI CHECKSUM
            logger.error("VNPAY response failed integrity check (Checksum).");
            model.addAttribute("message", "Lỗi bảo mật khi xác thực kết quả thanh toán.");
            return "user/vnpay-error";
        }
    }

    @PostMapping("/placeOrder")
    @ResponseBody // Giữ @ResponseBody cho phương thức API này
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
            // e.printStackTrace(); // Log lỗi chi tiết nếu cần
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        Map<String, Object> jsonResponse = new HashMap<>();

        if ("cod".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            jsonResponse.put("status", "success");
            // Sửa redirect URL (thêm dấu / ở đầu)
            jsonResponse.put("redirectUrl", "/order/success"); // Đảm bảo có dấu /
            return ResponseEntity.ok(jsonResponse);
        } else if ("bank_transfer".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            if (newOrder.getTotal() == null || newOrder.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Tổng tiền của đơn hàng không hợp lệ."));
            }
            long amount = newOrder.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
            String vnp_TxnRef = newOrder.getId().toString();
            String vnp_IpAddr = vnPayConfig.getIpAddress(request);
            String vnp_OrderInfo = "Thanh toan don hang " + vnp_TxnRef;
            try {
                String paymentUrl = vnPayConfig.createPaymentUrl(vnp_TxnRef, amount, vnp_OrderInfo, vnp_IpAddr);
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

    // ===>>> PHƯƠNG THỨC MỚI CHO TRANG SUCCESS <<<===
    @GetMapping("/order/success")
    public String orderSuccessPage() {
        // Đơn giản chỉ trả về tên view
        return "user/order-success"; // Trả về templates/user/order-success.html
    }
    // ===>>> KẾT THÚC <<<===
    

}