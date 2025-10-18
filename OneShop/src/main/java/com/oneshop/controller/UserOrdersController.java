package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.oneshop.entity.User;
import com.oneshop.service.OrderService;

import java.util.List;

@Controller
@RequestMapping("/user/orders")
public class UserOrdersController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public String listOrders(@AuthenticationPrincipal User user, Model model) {
        List<com.oneshop.entity.Order> orders = orderService.getUserOrders(user.getUserId()); // Giả sử thêm method getUserOrders trong OrderService
        model.addAttribute("orders", orders);
        return "user/orders";
    }
}