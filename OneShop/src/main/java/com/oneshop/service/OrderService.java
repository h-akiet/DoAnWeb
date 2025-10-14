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

    public List<Order> getAssignedOrders(Long shipperId) {
        return orderRepository.findByShipperId(shipperId);
    }

    public Map<String, Long> getOrderStats(Long shipperId) {
        List<Order> orders = getAssignedOrders(shipperId);
        return orders.stream().collect(Collectors.groupingBy(Order::getOrderStatus, Collectors.counting()));

    }
}