package com.oneshop.config; // Hoặc package config của bạn

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Lớp cấu hình chứa các hằng số và hàm tiện ích cho VNPAY.
 */
public class VnPayConfig {

    // THAY THẾ BẰNG THÔNG TIN CỦA BẠN LẤY TỪ VNPAY SANDBOX
    public static final String VNP_TMNCODE = "YOUR_TMN_CODE"; // Mã website (Terminal ID)
    public static final String VNP_HASHSECRET = "YOUR_HASH_SECRET"; // Chuỗi bí mật

    // URL của VNPAY Sandbox
    public static final String VNP_PAYURL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    
    // URL VNPAY sẽ gọi về server của bạn sau khi thanh toán
    // (Phải khớp với @GetMapping("/vnpay/return") trong VnPayController)
    public static final String VNP_RETURNURL_BASE = "http://localhost:8080/vnpay/return"; 

    // Phiên bản API
    public static final String VNP_VERSION = "2.1.0";
    // Lệnh thanh toán
    public static final String VNP_COMMAND = "pay";
    // Loại hàng hóa (để 'other' cho chung)
    public static final String VNP_ORDERTYPE = "other";
    // Mã tiền tệ
    public static final String VNP_CURRCODE = "VND";
    // Ngôn ngữ
    public static final String VNP_LOCALE = "vn";


    /**
     * Hàm băm HMAC-SHA512
     */
    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException();
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Hàm lấy địa chỉ IP của client
     */
    public static String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }
        // Trường hợp IP là localhost (::1 hoặc 127.0.0.1) VNPAY sẽ từ chối
        // -> Trả về một IP public ngẫu nhiên (chỉ dùng cho test)
        if ("127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "13.160.92.202"; // Một IP public ngẫu nhiên
        }
        return ipAddress;
    }

    /**
     * Hàm tạo chuỗi hash_data từ Map các tham số
     * Dùng để tạo vnp_SecureHash
     */
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(fieldValue);
            }
            if (itr.hasNext()) {
                hashData.append('&');
            }
        }
        return hmacSHA512(VNP_HASHSECRET, hashData.toString());
    }

    
    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}