package com.oneshop.entity;

/**
 * Enum định nghĩa các trạng thái của đơn hàng.
 * Tên file: OrderStatus.java
 */
public enum OrderStatus {
    PENDING,        // Chờ thanh toán (đơn hàng vừa tạo, chờ VNPAY/COD)
    CONFIRMED,      // Đã xác nhận (Shop đã thấy đơn, đang chuẩn bị hàng)
    DELIVERING,        // Đang giao (Đã bàn giao cho shipper)
    DELIVERED,      // Đã giao thành công
    CANCELLED,      // Đã hủy (bởi khách hoặc shop)
    RETURNED        // Trả hàng
}
