package com.oneshop.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.oneshop.config.VnPayConfig;
import com.oneshop.dto.PlaceOrderRequest; // <-- [SỬA] Import DTO từ package mới
import com.oneshop.entity.Order;
import com.oneshop.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class OrderController {

    @Autowired
    private OrderService orderService;
    
    // [SỬA] Đã xóa class PlaceOrderRequest lồng bên trong

    /**
     * Endpoint xử lý yêu cầu đặt hàng từ frontend (AJAX)
     */
    @PostMapping("/placeOrder")
    @ResponseBody // Đảm bảo phương thức này luôn trả về nội dung của Response Body
    public ResponseEntity<?> placeOrder(
            @RequestBody PlaceOrderRequest orderRequest, // <-- Sử dụng DTO đã được tách riêng
            HttpServletRequest request,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập để tiếp tục."));
        }
        String username = principal.getName();
        
        Order newOrder;
        try {
            // BƯỚC 1: Gọi service để tạo đơn hàng với trạng thái "PENDING"
            // Logic phức tạp sẽ nằm trong OrderService
            newOrder = orderService.createOrderFromRequest(username, orderRequest);
            
            if (newOrder == null) {
                 // Ném lỗi nếu service trả về null mà không có exception
                 throw new RuntimeException("Không thể tạo đơn hàng do lỗi không xác định.");
            }

        } catch (Exception e) {
            // Bắt tất cả các lỗi nghiệp vụ (hết hàng, sai địa chỉ...) từ service
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }

        // BƯỚC 2: Lấy thông tin từ đơn hàng vừa tạo để xử lý thanh toán
 //       long grandTotal = newOrder.getGrandTotal();
        String orderId = newOrder.getId().toString();
        
        Map<String, Object> jsonResponse = new HashMap<>();

        // BƯỚC 3: Phân nhánh logic dựa trên phương thức thanh toán
        if ("cod".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            // 3a. THANH TOÁN KHI NHẬN HÀNG (COD)
            jsonResponse.put("status", "success");
            jsonResponse.put("redirectUrl", "/order/success"); // Chuyển hướng tới trang đặt hàng thành công
            return ResponseEntity.ok(jsonResponse);

        } else if ("bank_transfer".equalsIgnoreCase(orderRequest.getPaymentMethod())) {
            // 3b. THANH TOÁN ONLINE QUA VNPAY
            
  //          long amount = grandTotal * 100; // VNPAY yêu cầu đơn vị là đồng
            String vnp_TxnRef = orderId;
            String vnp_IpAddr = VnPayConfig.getIpAddress(request);

            Map<String, String> vnp_Params = new HashMap<>();
            vnp_Params.put("vnp_Version", VnPayConfig.VNP_VERSION);
            vnp_Params.put("vnp_Command", VnPayConfig.VNP_COMMAND);
            vnp_Params.put("vnp_TmnCode", VnPayConfig.VNP_TMNCODE);
      //      vnp_Params.put("vnp_Amount", String.valueOf(amount));
            vnp_Params.put("vnp_CurrCode", VnPayConfig.VNP_CURRCODE);
            vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
            vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang #" + vnp_TxnRef);
            vnp_Params.put("vnp_OrderType", VnPayConfig.VNP_ORDERTYPE);
            vnp_Params.put("vnp_Locale", VnPayConfig.VNP_LOCALE);
            vnp_Params.put("vnp_ReturnUrl", VnPayConfig.VNP_RETURNURL_BASE);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnp_CreateDate = formatter.format(cld.getTime());
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

            // Build query URL
            StringBuilder query = new StringBuilder();
            try {
                for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
                    query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
                    query.append('&');
                }
                query.deleteCharAt(query.length() - 1); // Xóa dấu '&' cuối cùng
            
                String vnp_SecureHash = VnPayConfig.hmacSHA512(VnPayConfig.VNP_HASHSECRET, query.toString());
                query.append("&vnp_SecureHash=").append(vnp_SecureHash);
            } catch (Exception e) {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi khi tạo URL thanh toán VNPAY."));
            }

            String paymentUrl = VnPayConfig.VNP_PAYURL + "?" + query.toString();

            jsonResponse.put("status", "pending_payment");
            jsonResponse.put("paymentUrl", paymentUrl);
            return ResponseEntity.ok(jsonResponse);
        }

        // Trường hợp phương thức thanh toán không hợp lệ
        return ResponseEntity.badRequest().body(Map.of("message", "Phương thức thanh toán không được hỗ trợ."));
    }
}