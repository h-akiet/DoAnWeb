package com.oneshop.controller;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import com.oneshop.entity.Order;
import com.oneshop.entity.OrderDetail;
import com.oneshop.entity.OrderItem;
import com.oneshop.entity.Product;
import com.oneshop.entity.User;
import com.oneshop.service.OrderService;
import com.oneshop.service.ReviewService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/user/orders/{orderId}/review")
    public String showReviewForm(@PathVariable Long orderId, Model model) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return "redirect:/user/orders?error=" + UriUtils.encode("Đơn hàng không tồn tại", StandardCharsets.UTF_8);
        }
        if (!"DELIVERED".equals(order.getOrderStatus())) {
            return "redirect:/user/orders?error=" + UriUtils.encode("Đơn hàng chưa được giao, không thể đánh giá", StandardCharsets.UTF_8);
        }

        // Khởi tạo items để tránh LazyInitializationException
        Hibernate.initialize(order.getOrderDetails());
        // Sao chép items để tránh ConcurrentModificationException
        List<OrderDetail> items = new ArrayList<>(order.getOrderDetails());
        List<Map<String, Object>> productReviews = new ArrayList<>();

        for (OrderDetail item : items) {
            Map<String, Object> productInfo = new HashMap<>();
            Product product = item.getProductVariant().getProduct();
            productInfo.put("product",  product);
            productInfo.put("reviewed", reviewService.isProductReviewed(orderId, item.getProductVariant().getProduct().getProductId()));
            productReviews.add(productInfo);
        }

        model.addAttribute("order", order);
        model.addAttribute("productReviews", productReviews);
        return "user/review-form";
    }

    @PostMapping("/user/orders/{orderId}/review/{productId}")
    public String submitReview(@PathVariable Long orderId,
                              @PathVariable Long productId,
                              @RequestParam Integer rating,
                              @RequestParam String comment,
                              @AuthenticationPrincipal User user) {
        try {
            reviewService.saveReview(orderId, productId, user, rating, comment);
            return "redirect:/user/orders/" + orderId + "/review?success=" + UriUtils.encode("Đánh giá thành công!", StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return "redirect:/user/orders/" + orderId + "/review?error=" + UriUtils.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }
}