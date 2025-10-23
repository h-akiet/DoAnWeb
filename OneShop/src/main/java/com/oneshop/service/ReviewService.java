package com.oneshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.oneshop.entity.ProductReview;
import com.oneshop.entity.Order;
import com.oneshop.entity.Product;
import com.oneshop.entity.User;

import com.oneshop.repository.ProductReviewRepository;
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductRepository;

import java.time.LocalDateTime;

@Service
public class ReviewService {

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    public void saveReview(Long orderId, Long productId, User user, Integer rating, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        // Kiểm tra quyền: Chỉ người mua mới đánh giá, order phải DELIVERED, và có item product trong order
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Bạn không có quyền đánh giá đơn hàng này!");
        }
        if (!"DELIVERED".equals(order.getOrderStatus())) {
            throw new IllegalStateException("Đơn hàng chưa được giao, không thể đánh giá!");
        }
        boolean hasProduct = order.getOrderDetails().stream()
        		.anyMatch(item -> item.getProductVariant().getProduct().getProductId().equals(productId));
        if (!hasProduct) {
            throw new IllegalArgumentException("Sản phẩm không có trong đơn hàng này!");
        }

        // Kiểm tra chưa có review cho product trong order này
        if (reviewRepository.findByOrderAndProduct(order, product).isPresent()) {
            throw new IllegalStateException("Sản phẩm này đã được đánh giá trong đơn hàng!");
        }

        // Tạo review
        ProductReview review = new ProductReview();
        review.setUser(user);
        review.setProduct(product);
        review.setOrder(order);
        review.setRating(rating);
        review.setComment(comment);
        review.setReviewDate(LocalDateTime.now()); // Thêm review_date

        reviewRepository.save(review);
    }

    public boolean isProductReviewed(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        if (order == null || product == null) {
            return false;
        }
        return reviewRepository.findByOrderAndProduct(order, product).isPresent();
    }
}