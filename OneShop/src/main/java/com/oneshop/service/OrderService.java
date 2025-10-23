package com.oneshop.service.vendor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.oneshop.entity.vendor.Order;
import com.oneshop.entity.vendor.OrderStatus;

import java.util.Optional;

public interface OrderService {

    /**
     * Lấy danh sách đơn hàng cho một shop, có thể lọc theo trạng thái (có phân trang)
     * @param shopId ID của shop
     * @param status Trạng thái đơn hàng (nếu null thì lấy tất cả)
     * @param pageable Thông tin phân trang
     */
    Page<Order> getOrdersByShop(Long shopId, Optional<OrderStatus> status, Pageable pageable);

    /**
     * Lấy chi tiết một đơn hàng
     * @param orderId ID đơn hàng
     * @param shopId ID của shop (để đảm bảo vendor chỉ xem được đơn của mình)
     */
    Order getOrderDetails(Long orderId, Long shopId);

    /**
     * Cập nhật trạng thái một đơn hàng
     * @param orderId ID đơn hàng
     * @param newStatus Trạng thái mới
     * @param shopId ID của shop (để bảo mật)
     */
    Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long shopId);
    
    long countNewOrdersByShop(Long shopId);
}