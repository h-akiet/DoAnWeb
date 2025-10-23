package com.oneshop.entity.vendor;

public enum OrderStatus {
    NEW,           // Đơn hàng mới
    CONFIRMED,     // Đã xác nhận (chờ lấy hàng)
    SHIPPING,      // Đang giao
    DELIVERED,     // Đã giao
    CANCELLED,     // Đã hủy
    RETURNED       // Trả hàng/Hoàn tiền
}