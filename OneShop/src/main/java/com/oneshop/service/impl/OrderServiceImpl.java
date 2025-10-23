package com.oneshop.service.vendor.impl;

import com.oneshop.entity.vendor.Order;
import com.oneshop.entity.vendor.OrderStatus;
import com.oneshop.repository.vendor.OrderRepository;
import com.oneshop.service.vendor.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public Page<Order> getOrdersByShop(Long shopId, Optional<OrderStatus> status, Pageable pageable) {
        // Sau này, shopId sẽ được lấy từ user đang đăng nhập
        if (status.isPresent()) {
            // Nếu có trạng thái, lọc theo trạng thái
            return orderRepository.findByShopIdAndStatus(shopId, status.get(), pageable);
        } else {
            // Nếu không có, lấy tất cả đơn hàng của shop
            return orderRepository.findByShopId(shopId, pageable);
        }
    }

    @Override
    public Order getOrderDetails(Long orderId, Long shopId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Kiểm tra bảo mật: Đảm bảo đơn hàng này thuộc về đúng shop đang xem
        if (!order.getShop().getId().equals(shopId)) {
            throw new SecurityException("Bạn không có quyền xem đơn hàng này");
        }
        
        // (Trong tương lai, bạn có thể load thêm OrderItems tại đây nếu cần)
        return order;
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, Long shopId) {
        Order order = getOrderDetails(orderId, shopId); // Đã bao gồm kiểm tra bảo mật

        // (Trong tương lai, bạn có thể thêm logic kiểm tra
        // ví dụ: không thể chuyển từ ĐÃ GIAO về MỚI)
        
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }
    @Override
    public long countNewOrdersByShop(Long shopId) {
        return orderRepository.countByShopIdAndStatus(shopId, OrderStatus.NEW);
    }
}