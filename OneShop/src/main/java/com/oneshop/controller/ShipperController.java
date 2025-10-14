package com.oneshop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.oneshop.service.OrderService;

import com.oneshop.entity.User;

@Controller
@RequestMapping("/shipper")
public class ShipperController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal User user, Model model) {
    	orderService.getAssignedOrders(user.getUserId());
    	orderService.getOrderStats(user.getUserId());

        return "shipper/orders";
    }
}