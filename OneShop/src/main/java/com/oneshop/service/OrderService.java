// src/main/java/com/oneshop/service/OrderService.java
package com.oneshop.service;

import com.oneshop.dto.PlaceOrderRequest;
import com.oneshop.entity.Order;
import com.oneshop.entity.OrderStatus;
// Không import User, ResponseStatusException, ... ở interface
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal; // <<< THÊM IMPORT
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {

    // --- Vendor ---
    Page<Order> getOrdersByShop(Long shopId, Optional<OrderStatus> status, Pageable pageable);
    Order getOrderDetails(Long orderId, Long shopId);
    Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long shopId);
    long countNewOrdersByShop(Long shopId);

    // ===>>> THÊM CÁC PHƯƠNG THỨC DOANH THU <<<===
    /**
     * Lấy tổng doanh thu của shop từ các đơn hàng đã giao.
     */
    BigDecimal getTotalRevenueByShop(Long shopId);

    /**
     * Lấy doanh thu tháng hiện tại của shop từ các đơn hàng đã giao.
     */
    BigDecimal getCurrentMonthRevenueByShop(Long shopId);

    /**
     * Đếm tổng số đơn hàng đã giao của shop.
     */
    long countDeliveredOrdersByShop(Long shopId);

    /**
     * Lấy dữ liệu doanh thu theo tháng cho biểu đồ (ví dụ: 6 tháng gần nhất).
     * @return Map với key là "YYYY-MM" và value là doanh thu tháng đó.
     */
    Map<String, BigDecimal> getMonthlyRevenueData(Long shopId, int months);
    // ===>>> KẾT THÚC PHẦN THÊM <<<===

    // --- User ---
    List<Order> findOrdersByCurrentUser(String username);
    Order createOrderFromRequest(String username, PlaceOrderRequest orderRequest);
    Order findOrderByIdAndUser(Long orderId, String username);
    void cancelOrder(Long orderId, String username);
    Order getOrderById(Long orderId); // Cần cho ReviewController

    // --- Shipper ---
    List<Order> getAssignedOrders(Long shipperId);
    Map<String, Long> getOrderStats(Long shipperId);
    void deliverOrder(Long orderId, Long shipperId);
}