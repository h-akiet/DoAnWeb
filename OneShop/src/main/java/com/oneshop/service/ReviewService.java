// src/main/java/com/oneshop/service/ReviewService.java
package com.oneshop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // <<< THÊM IMPORT NÀY

import com.oneshop.entity.ProductReview;
import com.oneshop.entity.Order;
import com.oneshop.entity.Product;
import com.oneshop.entity.User;
import com.oneshop.entity.OrderStatus; // <<< THÊM IMPORT NÀY

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

    // Thêm @Transactional để đảm bảo lưu thành công
    @Transactional(rollbackFor = Exception.class) 
    public void saveReview(Long orderId, Long productId, User user, Integer rating, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại!"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại!"));

        // Kiểm tra quyền: Chỉ người mua mới đánh giá
        if (!order.getUser().getUserId().equals(user.getUserId())) {
            throw new SecurityException("Bạn không có quyền đánh giá đơn hàng này!");
        }

        // ===>>> SỬA LỖI KIỂM TRA TRẠNG THÁI <<<===
        // So sánh Enum với Enum, không so sánh String với Enum
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Đơn hàng chưa được giao, không thể đánh giá!");
        }
        // ===>>> KẾT THÚC SỬA LỖI <<<===

        // Kiểm tra có item product trong order
        boolean hasProduct = order.getOrderDetails().stream()
                .anyMatch(item -> item.getProductVariant() != null && 
                                  item.getProductVariant().getProduct() != null &&
                                  item.getProductVariant().getProduct().getProductId().equals(productId));
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
        review.setReviewDate(LocalDateTime.now());

        reviewRepository.save(review);
        
        // (Tùy chọn): Cập nhật lại rating trung bình cho sản phẩm ngay lập tức
        // double avgRating = reviewRepository.findAverageRatingByProductId(productId);
        // int reviewCount = reviewRepository.countReviewsByProductId(productId);
        // product.setRating(avgRating);
        // product.setReviewCount(reviewCount);
        // productRepository.save(product); // Cần đảm bảo hàm này không kích hoạt logic gì khác
    }

    @Transactional(readOnly = true) // Thêm @Transactional
    public boolean isProductReviewed(Long orderId, Long productId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        Product product = productRepository.findById(productId).orElse(null);
        if (order == null || product == null) {
            return false;
        }
        return reviewRepository.findByOrderAndProduct(order, product).isPresent();
    }
}