package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.oneshop.service.OrderService;
import com.oneshop.entity.User;
import com.oneshop.entity.Order;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal User user, Model model) {
        List<Order> orders = orderService.getAssignedOrders(user.getUserId());
        Map<String, Long> stats = orderService.getOrderStats(user.getUserId());

        model.addAttribute("orders", orders);
        model.addAttribute("stats", stats);

        return "shipper/orders";
    }

    @PostMapping("/orders/{id}/deliver")
    public String deliverOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
        orderService.deliverOrder(id, user.getUserId());
        return "redirect:/shipper/orders";
    }
}