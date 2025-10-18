package com.oneshop.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneshop.entity.Order;
import com.oneshop.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    public List<Order> getAssignedOrders(Long shipperId) {
        return orderRepository.findByShipperId(shipperId);
    }

    public Map<String, Long> getOrderStats(Long shipperId) {
        List<Order> orders = getAssignedOrders(shipperId);
        return orders.stream().collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));
    }

    public void deliverOrder(Long orderId, Long shipperId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));

        if (order.getShipper() == null || !order.getShipper().getUserId().equals(shipperId)) {
            throw new SecurityException("Bạn không có quyền cập nhật đơn hàng này!");
        }

        order.setOrderStatus("DELIVERED");
        orderRepository.save(order);

        // Gửi email thông báo đến người dùng sau khi cập nhật thành công
        emailService.sendDeliveryConfirmation(order.getUser().getEmail(), order.getId());
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId); // Giả sử thêm method findByUserId trong OrderRepository
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));
    }
}